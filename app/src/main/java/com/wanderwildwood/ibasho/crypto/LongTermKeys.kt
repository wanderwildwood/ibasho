package com.wanderwildwood.ibasho.crypto

import com.wanderwildwood.ibasho.crypto.CryptoV2.Companion.CLIENT_ITEM_ID_SIZE_BYTES
import com.wanderwildwood.ibasho.crypto.CryptoV2.Companion.hkdfDerive
import com.wanderwildwood.ibasho.utils.CypherUtils
import com.wanderwildwood.ibasho.utils.CypherUtils.AES_GCM_IV_SIZE_BYTES
import com.wanderwildwood.ibasho.utils.CypherUtils.AES_GCM_KEY_SIZE_BYTES
import com.wanderwildwood.ibasho.utils.CypherUtils.AES_GCM_TAG_SIZE_BYTES
import org.bouncycastle.util.Arrays

class LongTermKeys(
    val username: String,

    val masterKey: ByteArray,

    // Key encryption keys for the data types. Pre-derived from the master key.
    val commandKey: ByteArray,
    val locationKey: ByteArray,
    val pictureKey: ByteArray,
) {
    companion object {
        fun fromMasterKey(
            username: String,
            masterKey: ByteArray,
        ): LongTermKeys {
            val commandInfo = CryptoV2.CTX_KEK_CMD + username.toHash()
            val commandKey = hkdfDerive(masterKey, commandInfo)

            val locationInfo = CryptoV2.CTX_KEK_LOC + username.toHash()
            val locationKey = hkdfDerive(masterKey, locationInfo)

            val pictureInfo = CryptoV2.CTX_KEK_PIC + username.toHash()
            val pictureKey = hkdfDerive(masterKey, pictureInfo)

            return LongTermKeys(username, masterKey, commandKey, locationKey, pictureKey)
        }

        fun generate(
            username: String,
        ): LongTermKeys {
            val masterKey = CypherUtils.generateSecureRandom(32) // 256 bit
            return fromMasterKey(username, masterKey)
        }

        fun decryptMasterKey(
            username: String,
            preMasterKey: ByteArray,
            encryptedMasterKey: ByteArray,
        ): LongTermKeys? {
            val ad = CryptoV2.CTX_MASTER + username.toHash()
            val masterKey =
                CypherUtils.decryptWithAes(encryptedMasterKey, ad, preMasterKey)
                    ?: return null
            return fromMasterKey(username, masterKey)
        }
    }

    fun encryptMasterKey(preMasterKey: ByteArray): ByteArray {
        val ad = CryptoV2.CTX_MASTER + username.toHash()
        return CypherUtils.encryptWithAes(masterKey, ad, preMasterKey)
    }

    fun getFingerprint(): String {
        return masterKey.toHash().toHexString()
    }

    /* ------- Data encryption/decryption ------- */

    private fun kekByType(type: DataBlobType): ByteArray {
        return when (type) {
            DataBlobType.Command -> commandKey
            DataBlobType.Location -> locationKey
            DataBlobType.Picture -> pictureKey
        }
    }

    fun encryptDataBlob(raw: ByteArray, type: DataBlobType): EncryptedDataBlob {
        val uniqueId = CypherUtils.generateSecureRandom(CLIENT_ITEM_ID_SIZE_BYTES)
        val unixMillis = System.currentTimeMillis()
        val adSuffix =
            type.label.toByteArray() + username.toHash() + uniqueId + unixMillis.toBigEndianByteArray()

        // Generate + encrypt the DEK
        val dek = CypherUtils.generateSecureRandom(32)
        val adDek = CryptoV2.CTX_DEK + adSuffix
        val encryptedDek = CypherUtils.encryptWithAes(dek, adDek, kekByType(type))

        // Encrypt the data
        val adData = CryptoV2.CTX_DATA + adSuffix
        val encryptedData = CypherUtils.encryptWithAes(raw, adData, dek)

        val ciphertext = Arrays.concatenate(encryptedDek, encryptedData)

        return EncryptedDataBlob(uniqueId, unixMillis, type.label, ciphertext)
    }

    /**
     * Decrypt the data blob.
     *
     * WARNING: This function DOES NOT VALIDATE the timestamp or the uniqueId.
     */
    fun decryptDataBlob(blob: EncryptedDataBlob): ByteArray? {
        // Parse in order to validate against the know types
        val type = DataBlobType.fromLabel(blob.type) ?: return null

        val offset = AES_GCM_IV_SIZE_BYTES + AES_GCM_KEY_SIZE_BYTES + AES_GCM_TAG_SIZE_BYTES
        val encryptedDek = Arrays.copyOfRange(blob.ciphertext, 0, offset)
        val encryptedData = Arrays.copyOfRange(blob.ciphertext, offset, blob.ciphertext.size)

        val adSuffix =
            type.label.toByteArray() + username.toHash() + blob.uniqueId + blob.unixMillis.toBigEndianByteArray()

        // Decrypt the DEK
        val adDek = CryptoV2.CTX_DEK + adSuffix

        val dek = CypherUtils.decryptWithAes(encryptedDek, adDek, kekByType(type))
            ?: return null

        // Decrypt the data
        val adData = CryptoV2.CTX_DATA + adSuffix
        val data = CypherUtils.decryptWithAes(encryptedData, adData, dek)

        return data
    }
}
