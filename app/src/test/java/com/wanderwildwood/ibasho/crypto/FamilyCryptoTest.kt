package com.wanderwildwood.ibasho.crypto

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.util.Base64

class FamilyCryptoTest {

    private fun b64(b: ByteArray) = Base64.getEncoder().encodeToString(b)
    private fun unb64(s: String) = Base64.getDecoder().decode(s)

    private fun kek(user: String, fill: Int) =
        FamilyCrypto.deriveIdentityKek(user, ByteArray(32) { fill.toByte() })

    @Test
    fun identityRoundTripAndWrongKeyOrName() {
        val k = kek("ada", 9)
        val id = FamilyCrypto.generateIdentity("ada", k)
        assertEquals(65, id.publicRaw.size)
        val again = FamilyCrypto.openIdentity("ada", k, id.encPrivate)
        assertNotNull(again)
        assertArrayEquals(id.publicRaw, again!!.publicRaw)

        assertNull(FamilyCrypto.openIdentity("ada", kek("ada", 8), id.encPrivate))
        assertNull(FamilyCrypto.openIdentity("tomas", k, id.encPrivate))
        assertNull(FamilyCrypto.openIdentity("ada", k, ByteArray(5)))
    }

    @Test
    fun sealedOpensForTheViewerOnlyAndOnlyAsFromTheOwner() {
        val ada = FamilyCrypto.generateIdentity("ada", kek("ada", 1))
        val tomas = FamilyCrypto.generateIdentity("tomas", kek("tomas", 2))
        val carol = FamilyCrypto.generateIdentity("carol", kek("carol", 3))
        val place = """{"lat":1.5,"lon":2.5,"date":1700000000000}""".toByteArray()
        val at = 1700000000123L

        val s = FamilyCrypto.seal("ada", ada.privateKey, "tomas", tomas.publicRaw, at, place)
        assertArrayEquals(place, FamilyCrypto.open("tomas", tomas.privateKey, "ada", ada.publicRaw, at, s))

        val bad = listOf(
            { FamilyCrypto.open("carol", carol.privateKey, "ada", ada.publicRaw, at, s) },
            { FamilyCrypto.open("tomas", tomas.privateKey, "carol", carol.publicRaw, at, s) },
            { FamilyCrypto.open("tomas", tomas.privateKey, "ada", ada.publicRaw, at + 1, s) },
            { FamilyCrypto.open("tomas", tomas.privateKey, "adaa", ada.publicRaw, at, s) },
            {
                val bent = s.copyOf(); bent[bent.size - 1] = (bent.last().toInt() xor 1).toByte()
                FamilyCrypto.open("tomas", tomas.privateKey, "ada", ada.publicRaw, at, bent)
            },
            { FamilyCrypto.open("tomas", tomas.privateKey, "ada", ada.publicRaw, at, s.copyOf(40)) },
        )
        for (attempt in bad) assertThrows(FamilyCrypto.BadSeal::class.java) { attempt() }

        val s2 = FamilyCrypto.seal("ada", ada.privateKey, "tomas", tomas.publicRaw, at, place)
        assertFalse("each seal is fresh", s.contentEquals(s2))
    }

    @Test
    fun pointsOffTheCurveAreRefused() {
        assertNull(FamilyCrypto.publicFromRaw(ByteArray(65).also { it[0] = 4 }))
        assertNull(FamilyCrypto.publicFromRaw(ByteArray(33)))
        val ada = FamilyCrypto.generateIdentity("ada", kek("ada", 1))
        val bent = ada.publicRaw.copyOf().also { it[64] = (it[64].toInt() xor 1).toByte() }
        assertNull(FamilyCrypto.publicFromRaw(bent))
        assertThrows(FamilyCrypto.BadSeal::class.java) {
            FamilyCrypto.seal("ada", ada.privateKey, "tomas", bent, 1L, ByteArray(1))
        }
    }

    @Test
    fun fingerprintIsShortAndStable() {
        val raw = ByteArray(65) { it.toByte() }
        val f = FamilyCrypto.fingerprint(raw)
        assertEquals(f, FamilyCrypto.fingerprint(raw.copyOf()))
        assertEquals(4, f.split(" ").size)
    }

    /** Made by the web page's code (ibasho-server web/test/family.test.ts): the bytes must agree. */
    @Test
    fun opensWhatTheWebPageSealed() {
        val text = javaClass.classLoader!!.getResource("family-vectors.json")!!.readText()
        val v = Gson().fromJson(text, JsonObject::class.java)
        val user = v["username"].asString
        val k = FamilyCrypto.deriveIdentityKek(user, unb64(v["masterKey64"].asString))
        val me = FamilyCrypto.openIdentity(user, k, unb64(v["encPrivate64"].asString))
        assertNotNull("the web-made identity must open", me)
        assertEquals(v["publicKey64"].asString, b64(me!!.publicRaw))
        val plain = FamilyCrypto.open(
            user, me.privateKey,
            v["owner"].asString, unb64(v["ownerPublic64"].asString),
            v["unixMillis"].asLong, unb64(v["sealed64"].asString),
        )
        assertEquals(v["plaintext"].asString, String(plain))
    }

    /**
     * The other direction: writes a place sealed here to build/family-vectors-kotlin.json, for the
     * web side's test to open (ibasho-server web/test/family.test.ts reads it when given the path).
     */
    @Test
    fun writesAVectorForTheWebPage() {
        val master = ByteArray(32) { (it * 7 + 3).toByte() }
        val viewer = "tomas"
        val viewerId = FamilyCrypto.generateIdentity(viewer, FamilyCrypto.deriveIdentityKek(viewer, master))
        val owner = FamilyCrypto.generateIdentity("ada", kek("ada", 5))
        val plaintext = """{"provider":"gps","lat":-12.25,"lon":45.5,"accuracy":6,"bat":64,"date":1700000100000}"""
        val at = 1700000100456L
        val sealed = FamilyCrypto.seal("ada", owner.privateKey, viewer, viewerId.publicRaw, at, plaintext.toByteArray())
        val out = JsonObject().apply {
            addProperty("note", "Made by FamilyCrypto.kt. tomas is the viewer; ada sealed this place for him.")
            addProperty("username", viewer)
            addProperty("masterKey64", b64(master))
            addProperty("encPrivate64", b64(viewerId.encPrivate))
            addProperty("publicKey64", b64(viewerId.publicRaw))
            addProperty("owner", "ada")
            addProperty("ownerPublic64", b64(owner.publicRaw))
            addProperty("unixMillis", at)
            addProperty("sealed64", b64(sealed))
            addProperty("plaintext", plaintext)
        }
        File("build").mkdirs()
        File("build/family-vectors-kotlin.json").writeText(Gson().toJson(out) + "\n")
    }
}
