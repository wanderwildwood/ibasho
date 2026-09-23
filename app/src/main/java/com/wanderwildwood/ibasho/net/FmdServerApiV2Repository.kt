package com.wanderwildwood.ibasho.net

import android.content.Context
import com.wanderwildwood.ibasho.crypto.CryptoV2
import com.wanderwildwood.ibasho.crypto.CryptoV2.Companion.CLIENT_ITEM_ID_SIZE_BYTES
import com.wanderwildwood.ibasho.crypto.DataBlobType
import com.wanderwildwood.ibasho.crypto.EncryptedDataBlob
import com.wanderwildwood.ibasho.crypto.LongTermKeys
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_MASTER_KEY
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V2_PASSWORD_KEY
import com.wanderwildwood.ibasho.data.FmdLocation
import com.wanderwildwood.ibasho.data.FmdPicture
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.net.interceptor.AuthorizationInterceptor
import com.wanderwildwood.ibasho.net.interceptor.UserAgentInterceptor
import com.wanderwildwood.ibasho.net.model.DataRequestResponse
import com.wanderwildwood.ibasho.net.model.EncryptedItem
import com.wanderwildwood.ibasho.net.model.LoginRequest
import com.wanderwildwood.ibasho.net.model.LoginResponse
import com.wanderwildwood.ibasho.net.model.PasswordRequest
import com.wanderwildwood.ibasho.net.model.PushUrlRequestResponse
import com.wanderwildwood.ibasho.net.model.RegisterRequest
import com.wanderwildwood.ibasho.net.model.ServerMessage
import com.wanderwildwood.ibasho.utils.SingletonHolder
import com.wanderwildwood.ibasho.utils.decodeBase64
import com.wanderwildwood.ibasho.utils.encodeBase64
import com.wanderwildwood.ibasho.utils.log
import com.wanderwildwood.ibasho.utils.toIsoDateTimeString
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

data class FmdServerApiV2RepoSpec(
    val context: Context,
)

class FmdServerApiV2Repository private constructor(
    spec: FmdServerApiV2RepoSpec,
) : FmdServerApiService {

    companion object :
        SingletonHolder<FmdServerApiV2Repository, FmdServerApiV2RepoSpec>(::FmdServerApiV2Repository) {

        val TAG = FmdServerApiV2Repository::class.simpleName
    }

    private val context = spec.context // for ease of access

    private var service: FmdServerApiV2Service
    private val settings = SettingsRepository.getInstance(spec.context)
    private val encryptedSettings = EncryptedSettingsRepository.getInstance(spec.context)

    private lateinit var ltk: LongTermKeys

    init {
        service = initService()
    }

    // Only exposed for the v2 migration
    fun initServicePub() {
        service = initService()
    }

    /**
     * Call this to re-initialize the service whenever the baseUrl or the accessToken has changed.
     */
    private fun initService(): FmdServerApiV2Service {
        // Try to load LongTermKeys (if possible)
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val masterKey = encryptedSettings.getString(KEY_FMDSERVER_V2_MASTER_KEY)
        if (username.isNotBlank() && masterKey.isNotBlank()) {
            ltk = LongTermKeys.fromMasterKey(username, masterKey.decodeBase64())
        }

        val baseUrl = (settings.get(Settings.SET_FMDSERVER_URL) as String).trimEnd('/')
        val accessToken = encryptedSettings.getCachedAccessToken()

        val okHttpBuilder = OkHttpClient.Builder()
            .addInterceptor(UserAgentInterceptor())

        if (accessToken.isNotBlank()) {
            okHttpBuilder.addInterceptor(AuthorizationInterceptor(accessToken))
        }
        /*
        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            okHttpBuilder.addNetworkInterceptor(logging)
        }
         */

        val retrofit = Retrofit.Builder()
            .baseUrl("${baseUrl}/api/v2/")
            .client(okHttpBuilder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(FmdServerApiV2Service::class.java)
    }

    private fun <T> doRequestWithCachedToken(
        doRequest: (service: FmdServerApiV2Service) -> Call<T>,
        listener: Listener<T>,
        errorListener: ErrorListener,
    ) {
        val call = doRequest(service)
        call.enqueue(listener) { error ->
            // We can only retry HTTP_UNAUTHORIZED errors
            if (error.statusCode != 401) {
                errorListener.onError(error)
                return@enqueue
            }

            refreshAccessToken({
                context.log().i(TAG, "Retrying original request")
                val call = doRequest(service)
                call.enqueue(listener, errorListener)
            }, errorListener)
        }
    }

    fun refreshAccessToken(
        listener: Listener<Unit>,
        errorListener: ErrorListener,
    ) {
        context.log().i(TAG, "Refreshing access token")
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val passwordKey =
            encryptedSettings.getString(KEY_FMDSERVER_V2_PASSWORD_KEY).decodeBase64()
        val authKey = CryptoV2.deriveAuthKey(username, passwordKey)

        val loginRequest = LoginRequest(
            username,
            authKey.encodeBase64(),
            ACCESS_TOKEN_VALIDITY_SECS,
        )
        service.login(loginRequest).enqueue(
            listener = { response ->
                // If refreshing succeeds, store the new token
                encryptedSettings.setCachedAccessToken(response.accessToken)
                service = initService()
                // Notify caller, they can retry the original request
                listener.onResponse(Unit)
            },
            // If refreshing fails, use the original error handler
            errorListener,
        )
    }

    override fun getKeyFingerprint(): String {
        return ltk.getFingerprint()
    }

    override fun checkConnection(
        listener: Listener<Unit>,
        errorListener: ErrorListener
    ) {
        // (Ab)use the getPushUrl endpoint to check that we can make authenticated requests to the server
        doRequestWithCachedToken(
            { srv -> srv.getPushUrl() },
            { _ -> listener.onResponse(Unit) },
            errorListener
        )
    }

    override fun login(
        username: String,
        password: String,
        listener: Listener<ProtoVersion>,
        errorListener: ErrorListener
    ) {
        service = initService()
        service.getSalt(username).enqueue(
            listener = { response ->
                if (response.protoVersion == FMD_SERVER_PROTO_V2) {
                    val salt = response.salt64.decodeBase64()
                    loginV2(username, password, salt, listener, errorListener)
                } else if (response.protoVersion == FMD_SERVER_PROTO_V1) {
                    val repoV1 =
                        FmdServerApiV1Repository.getInstance(FmdServerApiV1RepoSpec(context))
                    repoV1.loginWithSalt(
                        username, password, response.salt64, listener, errorListener
                    )
                } else {
                    context.log().e(TAG, "Bad response: $response")
                    errorListener.onError("Unknown protocol version: ${response.protoVersion}")
                }
            },
            errorListener,
        )
    }

    private fun loginV2(
        username: String,
        password: String,
        salt: ByteArray,
        listener: Listener<ProtoVersion>,
        errorListener: ErrorListener
    ) {
        val passwordHashResult = CryptoV2.hashPassword(username, password, salt)

        val listener = { response: LoginResponse ->
            val ltk = LongTermKeys.decryptMasterKey(
                username,
                passwordHashResult.preMasterKey,
                response.encMasterKey64.decodeBase64()
            )
            if (ltk == null) {
                val msg = "Failed to decrypt master key"
                context.log().e(TAG, msg)
                errorListener.onError(msg)
            } else {
                this.ltk = ltk
                encryptedSettings.putString(
                    KEY_FMDSERVER_V2_MASTER_KEY,
                    ltk.masterKey.encodeBase64()
                )
                encryptedSettings.putString(
                    KEY_FMDSERVER_V2_PASSWORD_KEY,
                    passwordHashResult.passwordKey.encodeBase64()
                )
                encryptedSettings.setCachedAccessToken(response.accessToken)
                settings.set(Settings.SET_FMDSERVER_ID, username)
                service = initService()

                listener.onResponse(FMD_SERVER_PROTO_V2)
            }
        }

        val loginRequest = LoginRequest(
            username,
            passwordHashResult.authKey.encodeBase64(),
            ACCESS_TOKEN_VALIDITY_SECS,
        )
        service.login(loginRequest).enqueue(listener, errorListener)
    }

    override fun logout() {
        service.logout().enqueue({}, {})
    }

    override fun register(
        username: String,
        password: String,
        registrationToken: String,
        listener: Listener<ProtoVersion>,
        errorListener: ErrorListener
    ) {
        service = initService()
        val passwordHashResult = CryptoV2.hashPassword(username, password)
        val ltk = LongTermKeys.generate(username)

        val registerRequest = RegisterRequest(
            username = username,
            salt64 = passwordHashResult.salt.encodeBase64(),
            passwordHash64 = passwordHashResult.authKey.encodeBase64(),
            protoVersion = FMD_SERVER_PROTO_V2,
            encMasterKey64 = ltk.encryptMasterKey(passwordHashResult.preMasterKey).encodeBase64(),
            registrationToken = registrationToken,
        )

        service.register(registerRequest).enqueue(
            { response ->
                settings.set(Settings.SET_FMDSERVER_ID, username)

                this.ltk = ltk
                encryptedSettings.putString(
                    KEY_FMDSERVER_V2_MASTER_KEY,
                    ltk.masterKey.encodeBase64()
                )
                encryptedSettings.putString(
                    KEY_FMDSERVER_V2_PASSWORD_KEY,
                    passwordHashResult.passwordKey.encodeBase64()
                )
                encryptedSettings.setCachedAccessToken(response.accessToken)
                service = initService()

                listener.onResponse(FMD_SERVER_PROTO_V2)
            },
            errorListener
        )
    }

    override fun unregister(
        listener: Listener<Unit>,
        errorListener: ErrorListener
    ) {
        doRequestWithCachedToken({ srv -> srv.deleteAccount() }, {
            settings.removeServerAccount()
            listener.onResponse(Unit)
        }, errorListener)
    }

    override fun registerPushEndpoint(
        url: String,
        errorListener: ErrorListener
    ) {
        val request = PushUrlRequestResponse(url)
        doRequestWithCachedToken({ srv -> srv.postPushUrl(request) }, {}, errorListener)
    }

    override fun changePassword(
        // oldPassword is ignored -- TODO: remove this parameter once V1 is gone
        oldPassword: String,
        newPassword: String,
        listener: Listener<Unit>,
        errorListener: ErrorListener
    ) {
        val username = settings.get(Settings.SET_FMDSERVER_ID) as String
        val passwordHashResult = CryptoV2.hashPassword(username, newPassword)

        val masterKey = encryptedSettings.getString(KEY_FMDSERVER_V2_MASTER_KEY).decodeBase64()
        val ltk = LongTermKeys.fromMasterKey(username, masterKey)
        val newEncMasterKey = ltk.encryptMasterKey(passwordHashResult.preMasterKey)

        val request = PasswordRequest(
            newSalt64 = passwordHashResult.salt.encodeBase64(),
            newPasswordHash64 = passwordHashResult.authKey.encodeBase64(),
            newEncMasterKey64 = newEncMasterKey.encodeBase64(),
        )
        doRequestWithCachedToken({ srv -> srv.postPassword(request) }, { response ->
            // Only persist if the change was accepted by the server
            encryptedSettings.putString(
                KEY_FMDSERVER_V2_PASSWORD_KEY,
                passwordHashResult.passwordKey.encodeBase64()
            )
            encryptedSettings.setCachedAccessToken(response.accessToken)
            service = initService()

            listener.onResponse(Unit)
        }, errorListener)
    }

    override fun getCommand(
        listener: Listener<String>,
        errorListener: ErrorListener
    ) {
        doRequestWithCachedToken(
            { srv -> srv.getData(DataBlobType.Command.label) },
            listener = { response ->
                // Sort by unixMillis to make sure to execute the commands in order
                val sorted = response.items.sortedBy { it.unixMillis }
                for (cmd in sorted) {
                    handleCommandResponse(cmd, listener, errorListener)
                }
            },
            errorListener,
        )
    }

    private fun handleCommandResponse(
        cmd: EncryptedItem,
        listener: Listener<String>,
        // Errors in this function are logged, but not returned.
        // Instead, delete the command and return, so that the caller can continue with the next command.
        errorListener: ErrorListener
    ) {
        val uniqueId = cmd.clientItemIdHex.hexToByteArray()
        if (uniqueId.size != CLIENT_ITEM_ID_SIZE_BYTES) {
            val errorMsg = "bad itemId length: ${uniqueId.size} != $CLIENT_ITEM_ID_SIZE_BYTES"
            context.log().e(TAG, errorMsg)
            deleteSingleDatum(DataBlobType.Command.label, cmd.clientItemIdHex)
            return
        }

        // This only needs to be strictly increasing, to prevent replay attacks.
        // It doesn't need to be "current", i.e. we don't care how far away from "now" this timestamp is.
        val lastCmdMillis =
            (settings.get(Settings.SET_FMDSERVER_LAST_CMD_MILLIS) as Number).toLong()
        if (cmd.unixMillis <= lastCmdMillis) {
            // Skip command, delete it from server, and carry on.
            // This may be a legitimate command that failed to be deleted from the server.
            val errorMsg =
                "Skipping command ${cmd.clientItemIdHex}, timestamp not increasing: ${cmd.unixMillis} <= $lastCmdMillis"
            context.log().e(TAG, errorMsg)
            deleteSingleDatum(DataBlobType.Command.label, cmd.clientItemIdHex)
            return
        }

        val dataBlob = EncryptedDataBlob(
            uniqueId = uniqueId,
            unixMillis = cmd.unixMillis,
            type = DataBlobType.Command.label,
            ciphertext = cmd.ciphertext64.decodeBase64(),
        )

        val cmdString = ltk.decryptDataBlob(dataBlob)?.decodeToString()
        if (cmdString == null) {
            val errorMsg = "failed to decrypt command"
            context.log().e(TAG, errorMsg)
            deleteSingleDatum(DataBlobType.Command.label, cmd.clientItemIdHex)
            return
        }

        context.log()
            .i(TAG, "Decrypted command=$cmdString time=${cmd.unixMillis.toIsoDateTimeString()}")
        settings.set(Settings.SET_FMDSERVER_LAST_CMD_MILLIS, cmd.unixMillis)
        listener.onResponse(cmdString)
    }

    override fun sendLocation(location: FmdLocation) {
        sendLocations(listOf(location))
    }

    fun sendLocations(
        locations: List<FmdLocation>,
        listener: Listener<Unit> = Listener {},
        errorListener: ErrorListener = ErrorListener {},
    ) {
        val raw = locations.map { it.encodeToJson().encodeToByteArray() }
        sendData(raw, DataBlobType.Location, listener, errorListener)
    }

    override fun sendPicture(picture: FmdPicture) {
        sendPictures(listOf(picture))
    }

    fun sendPictures(
        pictures: List<FmdPicture>,
        listener: Listener<Unit> = Listener {},
        errorListener: ErrorListener = ErrorListener {},
    ) {
        val raw = pictures.map { it.encodeToJson().encodeToByteArray() }
        sendData(raw, DataBlobType.Picture, listener, errorListener)
    }

    private fun sendData(
        rawItems: List<ByteArray>,
        type: DataBlobType,
        listener: Listener<Unit> = Listener {},
        errorListener: ErrorListener = ErrorListener {},
    ) {
        val encItems = rawItems.map { raw ->
            val enc = ltk.encryptDataBlob(raw, type)
            EncryptedItem(
                clientItemIdHex = enc.uniqueId.toHexString(),
                unixMillis = enc.unixMillis,
                ciphertext64 = enc.ciphertext.encodeBase64(),
            )
        }

        val request = DataRequestResponse(encItems)
        doRequestWithCachedToken(
            { srv -> srv.postData(type.label, request) },
            listener = listener,
            errorListener = errorListener,
        )
    }

    fun deleteSingleDatum(dataType: String, clientItemIdHex: String) {
        doRequestWithCachedToken({ srv -> srv.deleteData(dataType, clientItemIdHex) }, {}, {})
    }

    fun getMessages(
        listener: Listener<List<ServerMessage>>,
        errorListener: ErrorListener
    ) {
        doRequestWithCachedToken(
            { srv -> srv.getMessages() },
            listener = { listener.onResponse(it.messages) },
            errorListener
        )
    }

    fun deleteSingleMessage(uuid: String) {
        doRequestWithCachedToken({ srv -> srv.deleteSingleMessage(uuid) }, {}, {})
    }
}

// Extension function that maps our Listener and ErrorListener to Retrofit's Callback.
fun <T> Call<T>.enqueue(listener: Listener<T>, errorListener: ErrorListener) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val body = response.body()
            if (body != null) {
                listener.onResponse(body)
            } else {
                val error = ServerError(
                    statusCode = response.code(),
                    body = response.errorBody()?.string(),
                    message = response.message()
                )
                errorListener.onError(error)
            }
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            errorListener.onError("Network exception: ${t.message}")
        }
    })
}
