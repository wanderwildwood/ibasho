package com.wanderwildwood.ibasho.crypto

import com.wanderwildwood.ibasho.crypto.CryptoV2.Companion.hkdfDerive
import com.wanderwildwood.ibasho.utils.CypherUtils
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECFieldFp
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPrivateKeySpec
import java.security.spec.ECPublicKeySpec
import javax.crypto.KeyAgreement

/**
 * Family sharing: the identity key pair and the sealed copies of a last known place.
 *
 * The server's web page implements the same bytes (web/src/lib/familycrypto.ts in ibasho-server);
 * both are checked against shared test vectors. See docs/PLAN-family.md for the design.
 *
 * Identity: a P-256 key pair per account. The public half is the 65-byte uncompressed point, sent
 * in the clear. The private half, d (32) || public (65), is encrypted with AES-256-GCM under
 *   kek = HKDF-SHA256(master key, info = "ibasho_v1_kek_identity" || SHA256(username))
 * with AD = "ibasho_v1_identity" || SHA256(username), stored as iv || ciphertext || tag. Every
 * login of the account can open it; the server cannot.
 *
 * A sealed copy from owner to viewer:
 *   0x01 || ephemeral public (65) || iv (12) || AES-256-GCM(ciphertext || tag)
 *   key = HKDF-SHA256(ECDH(ephemeral, viewer) || ECDH(owner, viewer),
 *                     info = "ibasho_v1_seal" || 0x00 || owner || 0x00 || viewer)
 *   AD  = "ibasho_v1_seal" || SHA256(owner) || SHA256(viewer) || unixMillis (8 bytes, big endian)
 * Only the viewer can open it, and only as coming from the owner at that time.
 */
object FamilyCrypto {

    private val CTX_KEK_IDENTITY = "ibasho_v1_kek_identity".toByteArray()
    private val CTX_IDENTITY = "ibasho_v1_identity".toByteArray()
    private val CTX_SEAL = "ibasho_v1_seal".toByteArray()

    const val POINT_BYTES = 65
    private const val SCALAR_BYTES = 32
    private const val IV_BYTES = 12
    private const val TAG_BYTES = 16
    private const val SEAL_VERSION: Byte = 1

    class Identity(
        /** The 65-byte uncompressed public point. */
        val publicRaw: ByteArray,
        val privateKey: PrivateKey,
        /** What the server keeps: the private half, encrypted. */
        val encPrivate: ByteArray,
    )

    class BadSeal(message: String) : Exception(message)

    private val params: ECParameterSpec by lazy {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec("secp256r1"))
        (gen.generateKeyPair().public as ECPublicKey).params
    }

    private fun sha256(b: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").digest(b)

    fun deriveIdentityKek(username: String, masterKey: ByteArray): ByteArray =
        hkdfDerive(masterKey, CTX_KEK_IDENTITY + sha256(username.toByteArray()))

    private fun identityAd(username: String) = CTX_IDENTITY + sha256(username.toByteArray())

    /** Makes a new identity for the account and wraps its private half under [kek]. */
    fun generateIdentity(username: String, kek: ByteArray): Identity {
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec("secp256r1"))
        val pair = gen.generateKeyPair()
        val publicRaw = encodePoint((pair.public as ECPublicKey).w)
        val d = fixed((pair.private as ECPrivateKey).s)
        val enc = CypherUtils.encryptWithAes(d + publicRaw, identityAd(username), kek)
            ?: throw IllegalStateException("AES-GCM unavailable")
        return Identity(publicRaw, privateFromScalar(d), enc)
    }

    /** Opens the identity the server holds. Returns null if the key is wrong or it was altered. */
    fun openIdentity(username: String, kek: ByteArray, encPrivate: ByteArray): Identity? {
        if (encPrivate.size < IV_BYTES + TAG_BYTES) return null
        val plain = CypherUtils.decryptWithAes(encPrivate, identityAd(username), kek) ?: return null
        if (plain.size != SCALAR_BYTES + POINT_BYTES) return null
        val d = plain.copyOfRange(0, SCALAR_BYTES)
        val publicRaw = plain.copyOfRange(SCALAR_BYTES, plain.size)
        decodePoint(publicRaw) ?: return null
        return Identity(publicRaw, privateFromScalar(d), encPrivate.copyOf())
    }

    /** Seals [plaintext] from [owner] to [viewer]. */
    fun seal(
        owner: String,
        ownerPrivate: PrivateKey,
        viewer: String,
        viewerPublicRaw: ByteArray,
        unixMillis: Long,
        plaintext: ByteArray,
    ): ByteArray {
        val viewerPub = publicFromRaw(viewerPublicRaw)
            ?: throw BadSeal("the viewer's key is not a P-256 point")
        val gen = KeyPairGenerator.getInstance("EC")
        gen.initialize(ECGenParameterSpec("secp256r1"))
        val eph = gen.generateKeyPair()
        val ephRaw = encodePoint((eph.public as ECPublicKey).w)
        val ikm = ecdh(eph.private, viewerPub) + ecdh(ownerPrivate, viewerPub)
        val key = hkdfDerive(ikm, sealInfo(owner, viewer))
        val body = CypherUtils.encryptWithAes(plaintext, sealAd(owner, viewer, unixMillis), key)
            ?: throw IllegalStateException("AES-GCM unavailable")
        // encryptWithAes returns iv || ciphertext || tag, which is exactly the tail of the format.
        return byteArrayOf(SEAL_VERSION) + ephRaw + body
    }

    /** Opens a copy [owner] sealed for [viewer]. Throws [BadSeal] if it is not exactly that. */
    fun open(
        viewer: String,
        viewerPrivate: PrivateKey,
        owner: String,
        ownerPublicRaw: ByteArray,
        unixMillis: Long,
        sealed: ByteArray,
    ): ByteArray {
        if (sealed.size < 1 + POINT_BYTES + IV_BYTES + TAG_BYTES || sealed[0] != SEAL_VERSION) {
            throw BadSeal("not a sealed place")
        }
        val eph = publicFromRaw(sealed.copyOfRange(1, 1 + POINT_BYTES))
            ?: throw BadSeal("bad ephemeral key")
        val ownerPub = publicFromRaw(ownerPublicRaw) ?: throw BadSeal("bad owner key")
        val ikm = ecdh(viewerPrivate, eph) + ecdh(viewerPrivate, ownerPub)
        val key = hkdfDerive(ikm, sealInfo(owner, viewer))
        val body = sealed.copyOfRange(1 + POINT_BYTES, sealed.size)
        return CypherUtils.decryptWithAes(body, sealAd(owner, viewer, unixMillis), key)
            ?: throw BadSeal("does not open as from $owner")
    }

    /** A short code two people can read to each other to check they hold the same key. */
    fun fingerprint(publicRaw: ByteArray): String {
        val h = sha256(publicRaw)
        return (0 until 4).joinToString(" ") { i ->
            ((h[2 * i].toInt() and 0xff) shl 8 or (h[2 * i + 1].toInt() and 0xff))
                .toString().padStart(5, '0')
        }
    }

    // ----- internals -----

    private fun sealInfo(owner: String, viewer: String) =
        CTX_SEAL + byteArrayOf(0) + owner.toByteArray() + byteArrayOf(0) + viewer.toByteArray()

    private fun sealAd(owner: String, viewer: String, unixMillis: Long) =
        CTX_SEAL + sha256(owner.toByteArray()) + sha256(viewer.toByteArray()) +
            unixMillis.toBigEndianByteArray()

    private fun ecdh(priv: PrivateKey, pub: PublicKey): ByteArray {
        val ka = KeyAgreement.getInstance("ECDH")
        ka.init(priv)
        ka.doPhase(pub, true)
        return fixed(BigInteger(1, ka.generateSecret()))
    }

    private fun privateFromScalar(d: ByteArray): PrivateKey =
        KeyFactory.getInstance("EC").generatePrivate(ECPrivateKeySpec(BigInteger(1, d), params))

    /** The public key for a raw point, or null if it is not a point on P-256. */
    fun publicFromRaw(raw: ByteArray): PublicKey? {
        val point = decodePoint(raw) ?: return null
        return KeyFactory.getInstance("EC").generatePublic(ECPublicKeySpec(point, params))
    }

    private fun decodePoint(raw: ByteArray): ECPoint? {
        if (raw.size != POINT_BYTES || raw[0] != 0x04.toByte()) return null
        val x = BigInteger(1, raw.copyOfRange(1, 1 + SCALAR_BYTES))
        val y = BigInteger(1, raw.copyOfRange(1 + SCALAR_BYTES, POINT_BYTES))
        val curve = params.curve
        val p = (curve.field as ECFieldFp).p
        if (x >= p || y >= p) return null
        // y^2 = x^3 + ax + b (mod p): refuse anything off the curve before it reaches ECDH.
        val left = y.modPow(BigInteger.valueOf(2), p)
        val right = x.modPow(BigInteger.valueOf(3), p).add(curve.a.multiply(x)).add(curve.b).mod(p)
        if (left != right) return null
        return ECPoint(x, y)
    }

    private fun encodePoint(w: ECPoint): ByteArray =
        byteArrayOf(0x04) + fixed(w.affineX) + fixed(w.affineY)

    /** A non-negative number as exactly 32 big-endian bytes. */
    private fun fixed(n: BigInteger): ByteArray {
        val b = n.toByteArray()
        return when {
            b.size == SCALAR_BYTES -> b
            b.size > SCALAR_BYTES -> b.copyOfRange(b.size - SCALAR_BYTES, b.size)
            else -> ByteArray(SCALAR_BYTES - b.size) + b
        }
    }
}
