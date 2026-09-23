package com.wanderwildwood.ibasho.push

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.wanderwildwood.ibasho.R
import com.wanderwildwood.ibasho.ui.settings.FMDServerActivity
import com.wanderwildwood.ibasho.utils.Notifications
import com.wanderwildwood.ibasho.utils.log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

/**
 * Holds the WebSocket to Mozilla's push server for the built-in distributor.
 *
 * A foreground service because nothing else survives on a phone that kills background
 * apps, and because a process in that state keeps its network through Doze. Its
 * notification is on a low-importance channel, so it sits quietly and never sounds.
 *
 * The connection is Sunup's (https://codeberg.org/Sunup/android, Apache-2.0, commit
 * fe768d0 "1.3.3", api/ServerConnection.kt and api/MessageSender.kt): the same OkHttp
 * client settings, hello with the stored uaid, registering pending channels after hello,
 * acking every notification, answering the server's pings and pinging it at most once a
 * minute. Reconnection follows the UnifiedPush distributor library that Sunup is built on
 * (https://codeberg.org/UnifiedPush/android-distributor, Apache-2.0, commit 6daaf14
 * "0.7.6"): a growing delay after each failure, run through WorkManager so it waits for
 * Doze's maintenance windows rather than fighting them, and a network callback that drops
 * the socket when the network goes and reconnects when it comes back.
 */
class PushConnectionService : Service() {

    private val lock = Any()
    private var socket: WebSocket? = null
    @Volatile
    private var open = false
    private var failures = 0
    private var lastPingMillis = 0L
    @Volatile
    private var waitingForPong = false
    private var connectWakeLock: PowerManager.WakeLock? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val handler = Handler(Looper.getMainLooper())

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .pingInterval(1, TimeUnit.MINUTES)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        log().i(TAG, "Push connection service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Always first: a service started with startForegroundService() has five seconds.
        startForeground(NOTIFICATION_ID, buildNotification())
        if (!EmbeddedPush.shouldRun(this)) {
            log().i(TAG, "Nothing to hold a connection for, stopping")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        registerNetworkCallback()
        connect()
        return START_STICKY
    }

    override fun onDestroy() {
        log().i(TAG, "Push connection service stopped")
        instance = null
        handler.removeCallbacksAndMessages(null)
        unregisterNetworkCallback()
        synchronized(lock) {
            socket?.close(1000, null)
            socket = null
            open = false
        }
        releaseConnectWakeLock()
        super.onDestroy()
    }

    val isOpen: Boolean get() = open

    fun connect() {
        synchronized(lock) {
            if (socket != null) return
            if (!hasNetwork()) {
                log().i(TAG, "No network, waiting for one")
                return
            }
            val store = EmbeddedPushStore(this)
            log().i(TAG, "Connecting to ${EmbeddedPush.PUSH_SERVER_URL} (known uaid: ${store.uaid != null})")
            acquireConnectWakeLock()
            val request = Request.Builder().url(EmbeddedPush.PUSH_SERVER_URL).build()
            val ws = client.newWebSocket(request, Listener())
            socket = ws
            // OkHttp queues this until the socket is open.
            ws.send(AutopushProtocol.Client.hello(store.uaid))
        }
    }

    /** Registers every channel the server has not given an endpoint yet. */
    fun registerPending() {
        val ws = synchronized(lock) { socket.takeIf { open } } ?: return
        EmbeddedPushStore(this).channels().filter { it.endpoint == null }.forEach {
            log().i(TAG, "Registering channel ${it.channelId}")
            ws.send(AutopushProtocol.Client.register(it.channelId, it.vapid))
        }
    }

    fun unregisterChannel(channelId: String) {
        val ws = synchronized(lock) { socket.takeIf { open } } ?: return
        ws.send(AutopushProtocol.Client.unregister(channelId))
    }

    /**
     * Checks the connection is still alive. The server silently drops a client that pings
     * too often, hence at most once a minute (Sunup's MessageSender.ping).
     */
    fun ping() {
        val ws = synchronized(lock) { socket.takeIf { open } } ?: return
        val now = System.currentTimeMillis()
        if (now - lastPingMillis < 60_000L) return
        lastPingMillis = now
        waitingForPong = true
        ws.send(AutopushProtocol.Client.PING)
    }

    private inner class Listener : WebSocketListener() {

        private fun isCurrent(ws: WebSocket) = synchronized(lock) { socket === ws }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (!isCurrent(webSocket)) return webSocket.cancel()
            log().i(TAG, "Connected (${response.code})")
            releaseConnectWakeLock()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (!isCurrent(webSocket)) return
            when (val message = AutopushProtocol.Server.parse(text)) {
                is AutopushProtocol.Server.Hello -> onHello(message)
                is AutopushProtocol.Server.Register -> onRegister(message)
                is AutopushProtocol.Server.Notification -> onNotification(webSocket, message)
                AutopushProtocol.Server.Ping -> {
                    if (waitingForPong) {
                        waitingForPong = false
                    } else {
                        webSocket.send(AutopushProtocol.Client.PING)
                    }
                }

                is AutopushProtocol.Server.Unregister ->
                    log().i(TAG, "Server unregistered channel ${message.channelId}")

                is AutopushProtocol.Server.Other -> {}
                null -> log().w(TAG, "Could not read a message from the push server")
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            onLost(webSocket, "Connection closed ($code)")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onLost(webSocket, "Connection failed: $t")
        }
    }

    private fun onHello(message: AutopushProtocol.Server.Hello) {
        val store = EmbeddedPushStore(this)
        synchronized(lock) {
            open = true
            failures = 0
        }
        if (message.uaid != store.uaid) {
            // The server knows this phone by a new id, so every endpoint it gave is void.
            log().i(TAG, "New uaid from the push server, registering every channel again")
            store.uaid = message.uaid
            store.forgetEndpoints()
        }
        registerPending()
    }

    private fun onRegister(message: AutopushProtocol.Server.Register) {
        val store = EmbeddedPushStore(this)
        val channel = store.channelForId(message.channelId) ?: return
        if (message.status == 200 && message.endpoint != null) {
            store.setEndpoint(channel.channelId, message.endpoint)
            EmbeddedPush.deliverEndpoint(this, channel.token, message.endpoint)
        } else {
            log().e(TAG, "Push server refused channel ${channel.channelId} (status ${message.status})")
            EmbeddedPush.deliverRegistrationFailed(this, channel.token)
        }
    }

    private fun onNotification(ws: WebSocket, message: AutopushProtocol.Server.Notification) {
        // Long enough for the connector to take the broadcast and start its own work.
        val wakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ibasho:push-message")
        wakeLock.acquire(10_000L)
        val channel = EmbeddedPushStore(this).channelForId(message.channelId)
        if (channel != null) {
            // A push with no body still means "look for a command": deliver it empty.
            val bytes = message.data?.let { Base64.decode(it, Base64.URL_SAFE) } ?: ByteArray(0)
            EmbeddedPush.deliverMessage(this, channel.token, bytes)
        } else {
            log().w(TAG, "Push message for unknown channel ${message.channelId}")
        }
        ws.send(AutopushProtocol.Client.ack(message.channelId, message.version))
    }

    private fun onLost(ws: WebSocket, why: String) {
        val delay = synchronized(lock) {
            // A socket already dropped on purpose (stopped, or the network went) is not news.
            if (socket !== ws) return
            socket = null
            open = false
            failures += 1
            AutopushProtocol.retryDelayMillis(failures)
        }
        log().w(TAG, why)
        releaseConnectWakeLock()
        if (!EmbeddedPush.shouldRun(this)) return
        if (!hasNetwork()) {
            log().i(TAG, "No network, will reconnect when it is back")
            return
        }
        log().i(TAG, "Reconnecting in ${delay / 1000}s (failure $failures)")
        PushRestartWorker.runOnce(this, delay)
    }

    private fun hasNetwork(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetwork != null
    }

    private fun registerNetworkCallback() {
        if (networkCallback != null) return
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                handler.postDelayed({
                    if (socket == null && EmbeddedPush.shouldRun(this@PushConnectionService)) {
                        log().i(TAG, "Network is back, reconnecting")
                        connect()
                    }
                }, 2_000L)
            }

            override fun onLost(network: Network) {
                // Drop the socket without counting a failure: it is the network, not the server.
                val ws = synchronized(lock) {
                    val ws = socket
                    socket = null
                    open = false
                    failures = 0
                    ws
                }
                if (ws != null) {
                    log().i(TAG, "Network lost, dropping the connection")
                    ws.cancel()
                }
            }
        }
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.registerDefaultNetworkCallback(callback)
        networkCallback = callback
    }

    private fun unregisterNetworkCallback() {
        val callback = networkCallback ?: return
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: IllegalArgumentException) {
        }
        networkCallback = null
    }

    /** Held from connecting until the socket opens or fails, as Sunup does, so Doze cannot cut it short. */
    private fun acquireConnectWakeLock() {
        releaseConnectWakeLock()
        connectWakeLock = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ibasho:push-connect")
            .apply { acquire(30_000L) }
    }

    private fun releaseConnectWakeLock() {
        synchronized(lock) {
            connectWakeLock?.let { if (it.isHeld) it.release() }
            connectWakeLock = null
        }
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, Notifications.CHANNEL_PUSH_CONNECTION.toString())
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.push_connection_notification_title))
            .setContentText(getString(R.string.push_connection_notification_text))
            .setStyle(NotificationCompat.BigTextStyle().bigText(getString(R.string.push_connection_notification_text)))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, FMDServerActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

    companion object {
        private const val TAG = "PushConnection"
        private const val NOTIFICATION_ID = 4711

        @Volatile
        var instance: PushConnectionService? = null
            private set

        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, PushConnectionService::class.java))
            } catch (e: IllegalStateException) {
                // Android 12 refuses to start a foreground service from the background unless
                // the app is exempt (battery optimisation off, or "display over other apps").
                // The periodic worker tries again; opening the app always works.
                context.log().w(TAG, "Could not start the push connection from the background: $e")
            }
        }

        fun stop(context: Context) {
            if (instance == null) return
            context.log().i(TAG, "Stopping the push connection")
            context.stopService(Intent(context, PushConnectionService::class.java))
        }
    }
}
