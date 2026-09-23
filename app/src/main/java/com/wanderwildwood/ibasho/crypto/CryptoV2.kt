package com.wanderwildwood.ibasho.crypto

import com.wanderwildwood.ibasho.utils.CypherUtils
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

class CryptoV2 {
    companion object {
        // Contexts for key schedule
        private val CTX_PASSWORD = "fmd_v2_password".toByteArray()
        private val CTX_AUTH = "fmd_v2_auth".toByteArray()
        private val CTX_PREMASTER = "fmd_v2_premaster".toByteArray()
        internal val CTX_MASTER = "fmd_v2_master".toByteArray()

        // Contexts for main KEKs
        internal val CTX_KEK_CMD = "fmd_v2_kek_command".toByteArray()
        internal val CTX_KEK_LOC = "fmd_v2_kek_location".toByteArray()
        internal val CTX_KEK_PIC = "fmd_v2_kek_picture".toByteArray()

        // Contexts for data encryption
        internal val CTX_DEK = "fmd_v2_dek_".toByteArray() // location, ...
        internal val CTX_DATA = "fmd_v2_data_".toByteArray() // location, ...

        internal const val CLIENT_ITEM_ID_SIZE_BYTES = 16 // 128 bit

        fun hkdfDerive(ikm: ByteArray, info: ByteArray): ByteArray {
            val outputLenBytes = 32
            val output = ByteArray(outputLenBytes)

            val hkdf = HKDFBytesGenerator(SHA256Digest())
            hkdf.init(HKDFParameters(ikm, null, info))
            hkdf.generateBytes(output, 0, output.size)

            return output
        }

        fun hashPassword(username: String, password: String): PasswordHashResult {
            val salt = CypherUtils.generateSecureRandom(CypherUtils.ARGON2_SALT_LENGTH)
            return hashPassword(username, password, salt)
        }

        /**
         * Derives K_auth and K_pmk from the user's password.
         * One initial Argon2 for hardness, then HKDF to split into subkeys.
         * See the FMD Server protocol key schedule.
         */
        fun hashPassword(username: String, password: String, salt: ByteArray): PasswordHashResult {
            val argonInput = CTX_PASSWORD + username.toHash() + password.toByteArray()
            val argonResult = CypherUtils.hashPasswordArgon2(argonInput, salt)
            val passwordKey = argonResult.hash

            val authInfo = CTX_AUTH + username.toHash()
            val authKey = hkdfDerive(passwordKey, authInfo)

            val preMasterInfo = CTX_PREMASTER + username.toHash()
            val preMasterKey = hkdfDerive(passwordKey, preMasterInfo)

            return PasswordHashResult(salt, passwordKey, authKey, preMasterKey)
        }

        fun deriveAuthKey(username: String, passwordKey: ByteArray): ByteArray {
            // Same as above
            val authInfo = CTX_AUTH + username.toHash()
            val authKey = hkdfDerive(passwordKey, authInfo)
            return authKey
        }
    }
}

fun ByteArray.toHash(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this)
}

fun String.toHash(): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(this.toByteArray())
}

fun Long.toBigEndianByteArray(): ByteArray {
    val buf = ByteBuffer.allocate(8)
    buf.order(ByteOrder.BIG_ENDIAN)
    buf.putLong(this)
    return buf.array()
}

class PasswordHashResult(
    val salt: ByteArray,
    val passwordKey: ByteArray,

    // Pre-derived K_auth and K_pmk
    val authKey: ByteArray,
    val preMasterKey: ByteArray,
)
