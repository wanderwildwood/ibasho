package com.wanderwildwood.ibasho.family

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wanderwildwood.ibasho.crypto.FamilyCrypto
import com.wanderwildwood.ibasho.data.FmdLocation
import com.wanderwildwood.ibasho.data.Settings
import com.wanderwildwood.ibasho.data.SettingsRepository
import com.wanderwildwood.ibasho.family.FamilyLogic.Seen
import com.wanderwildwood.ibasho.net.FMD_SERVER_PROTO_V2
import com.wanderwildwood.ibasho.net.FmdServerApiV2RepoSpec
import com.wanderwildwood.ibasho.net.FmdServerApiV2Repository
import com.wanderwildwood.ibasho.net.FmdServerApiV2Service
import com.wanderwildwood.ibasho.net.ServerError
import com.wanderwildwood.ibasho.net.model.IdentityBody
import com.wanderwildwood.ibasho.net.model.PauseBody
import com.wanderwildwood.ibasho.net.model.SealedPut
import com.wanderwildwood.ibasho.net.model.SealedPutRequest
import com.wanderwildwood.ibasho.utils.SingletonHolder
import com.wanderwildwood.ibasho.utils.decodeBase64
import com.wanderwildwood.ibasho.utils.encodeBase64
import com.wanderwildwood.ibasho.utils.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Call
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Family sharing on this phone: the identity key, whom this phone shares with, the sealed copy
 * sent after each regular upload, and what others share with this phone. See docs/PLAN-family.md.
 *
 * No work of its own is scheduled: sharing rides on the regular upload, and looking happens when
 * the Family screen asks.
 */
class FamilyRepository private constructor(private val context: Context) {

    companion object : SingletonHolder<FamilyRepository, Context>({ FamilyRepository(it.applicationContext) }) {
        private const val TAG = "Family"
    }

    class FamilyError(val error: ServerError) : Exception(error.message)

    /** One row on the Family screen. */
    data class Row(
        val person: FamilyPerson,
        val seen: Seen,
        /** This phone shares with them. */
        val chosen: Boolean,
        /** This phone stopped sealing to them because their key changed. */
        val keyChanged: Boolean,
    )

    data class Look(val me: String, val rows: List<Row>)

    private val settings = SettingsRepository.getInstance(context)
    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    @Volatile
    private var identity: Pair<String, FamilyCrypto.Identity>? = null

    private val api get() = FmdServerApiV2Repository.getInstance(FmdServerApiV2RepoSpec(context))

    /** Family sharing needs a server account on the current protocol. */
    fun isAvailable(): Boolean =
        settings.serverAccountExists() &&
            (settings.get(Settings.SET_FMD_CRYPT_PROTO) as Number).toInt() == FMD_SERVER_PROTO_V2

    var isSharing: Boolean
        get() = settings.get(Settings.SET_FAMILY_SHARING) as Boolean
        private set(v) = settings.set(Settings.SET_FAMILY_SHARING, v)

    val chosen: Set<String>
        get() = readJson(Settings.SET_FAMILY_AUDIENCE, object : TypeToken<Set<String>>() {}) ?: emptySet()

    private var pins: Map<String, String>
        get() = readJson(Settings.SET_FAMILY_PINS, object : TypeToken<Map<String, String>>() {}) ?: emptyMap()
        set(v) = settings.set(Settings.SET_FAMILY_PINS, gson.toJson(v))

    private var keyChanged: Set<String>
        get() = readJson(Settings.SET_FAMILY_KEY_CHANGED, object : TypeToken<Set<String>>() {}) ?: emptySet()
        set(v) = settings.set(Settings.SET_FAMILY_KEY_CHANGED, gson.toJson(v))

    private fun <T> readJson(key: Int, type: TypeToken<T>): T? {
        val s = settings.get(key) as String
        if (s.isEmpty()) return null
        return runCatching { gson.fromJson<T>(s, type.type) }.getOrNull()
    }

    // ----- network -----

    private suspend fun <T> call(request: (FmdServerApiV2Service) -> Call<T>): T =
        suspendCancellableCoroutine { cont ->
            api.familyRequest(
                request,
                { cont.resume(it) },
                { cont.resumeWithException(FamilyError(it)) },
            )
        }

    /** This account's identity: opened from the server, or made now if it has none yet. */
    private suspend fun myIdentity(): Pair<String, FamilyCrypto.Identity> {
        val keys = api.longTermKeysOrNull() ?: throw IllegalStateException("not logged in")
        val me = keys.username
        identity?.takeIf { it.first == me }?.let { return it }
        val kek = FamilyCrypto.deriveIdentityKek(me, keys.masterKey)

        fun adopt(body: IdentityBody): FamilyCrypto.Identity =
            FamilyCrypto.openIdentity(me, kek, body.encPrivateKey64.decodeBase64())
                ?: throw IllegalStateException("the identity on the server does not open with this account")

        val id = try {
            adopt(call { it.getIdentity() })
        } catch (e: FamilyError) {
            if (e.error.statusCode != 404) throw e
            val made = FamilyCrypto.generateIdentity(me, kek)
            val body = IdentityBody(made.publicRaw.encodeBase64(), made.encPrivate.encodeBase64())
            try {
                call { it.postIdentity(body) }
                made
            } catch (e2: FamilyError) {
                // Another device of this account made one first; the first one wins.
                if (e2.error.statusCode != 409 || e2.error.body == null) throw e2
                adopt(gson.fromJson(e2.error.body, IdentityBody::class.java))
            }
        }
        return (me to id).also { identity = it }
    }

    private suspend fun family(): Pair<Boolean, List<FamilyPerson>> {
        val r = call { it.getFamily() }
        val people = r.people.orEmpty().map { FamilyPerson(it.username, it.publicKey64.orEmpty(), it.paused) }
        return r.me.paused to people
    }

    // ----- sharing -----

    /** After a regular upload: seal the same place for each chosen person. Never throws. */
    fun shareInBackground(location: FmdLocation) {
        if (!isSharing || !isAvailable()) return
        scope.launch {
            try {
                mutex.withLock { share(location) }
            } catch (e: Exception) {
                context.log().w(TAG, "Sharing with family failed: ${e.message}")
            }
        }
    }

    private suspend fun share(location: FmdLocation) {
        if (!isSharing) return
        val (me, id) = myIdentity()
        val (paused, people) = family()
        if (paused) {
            // Switched off elsewhere (the web page): follow it.
            isSharing = false
            return
        }
        val plan = FamilyLogic.plan(me, people, chosen, pins)
        val plain = location.encodeToJson().toByteArray()
        val items = plan.sealTo.map { p ->
            SealedPut(
                viewer = p.name,
                unixMillis = location.timeMillis,
                ciphertext64 = FamilyCrypto.seal(
                    me, id.privateKey, p.name, p.publicKey64.decodeBase64(),
                    location.timeMillis, plain,
                ).encodeBase64(),
            )
        }
        pins = plan.pins
        keyChanged = plan.keyChanged.toSet()
        call { it.postFamilyLocations(SealedPutRequest(items, plan.audience)) }
        context.log().i(TAG, "Shared the last place with ${items.size} of ${plan.audience.size} chosen")
    }

    /** Sends the chosen list now (with the last known place, if there is one). */
    private suspend fun shareNow() {
        val last = settings.getLastKnownLocation()
        if (last != null) {
            share(last)
        } else if (isSharing) {
            myIdentity()
            val plan = FamilyLogic.plan(identity!!.first, family().second, chosen, pins)
            call { it.postFamilyLocations(SealedPutRequest(emptyList(), plan.audience)) }
        }
    }

    /** Turns sharing on or off. Off takes every copy off the server at once. */
    suspend fun setSharing(on: Boolean) = mutex.withLock {
        call { it.postFamilyPause(PauseBody(paused = !on)) }
        isSharing = on
        if (on) shareNow()
    }

    /** Ticks or unticks a person. Unticking takes their copy off the server now. */
    suspend fun setChosen(name: String, on: Boolean) = mutex.withLock {
        val next = if (on) chosen + name else chosen - name
        settings.set(Settings.SET_FAMILY_AUDIENCE, gson.toJson(next))
        if (isSharing) shareNow()
    }

    /** Trusts a person's new key (after a reinstall, say): the next look pins it again. */
    suspend fun acceptNewKey(name: String) = mutex.withLock {
        pins = pins - name
        keyChanged = keyChanged - name
        if (isSharing) shareNow()
    }

    // ----- looking -----

    /** Who is here, and the last place of each person who shares with this phone. */
    suspend fun look(): Look = mutex.withLock {
        val (me, id) = myIdentity()
        val (paused, people) = family()
        if (paused && isSharing) isSharing = false
        val copies = call { it.getFamilyLocations() }.items.orEmpty().associateBy { it.owner }
        val pinned = FamilyLogic.pinOnSight(people, pins)
        pins = pinned
        val chosenNow = chosen
        val changed = keyChanged
        val rows = people.sortedBy { it.name }.map { p ->
            val copy = copies[p.name]
            val seen = FamilyLogic.before(p, copy != null, pinned) ?: try {
                val plain = FamilyCrypto.open(
                    me, id.privateKey, p.name, p.publicKey64.decodeBase64(),
                    copy!!.unixMillis, copy.ciphertext64.decodeBase64(),
                )
                val loc = FmdLocation.decodeFromJson(String(plain))
                if (loc == null || !loc.lat.isFinite() || !loc.lon.isFinite()) Seen.Unreadable
                else Seen.Place(loc.lat, loc.lon, loc.accuracy, loc.batteryLevel, loc.timeMillis)
            } catch (e: FamilyCrypto.BadSeal) {
                Seen.Unreadable
            }
            Row(p, seen, p.name in chosenNow, p.name in changed)
        }
        Look(me, rows)
    }

    /** The fingerprint of a person's pinned key, to compare in person. */
    fun fingerprintOf(person: FamilyPerson): String? =
        (pins[person.name] ?: person.publicKey64.ifEmpty { null })
            ?.let { FamilyCrypto.fingerprint(it.decodeBase64()) }

    /** Called on log-out: forget the cached identity. */
    fun forget() {
        identity = null
    }
}
