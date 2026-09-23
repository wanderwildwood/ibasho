package com.wanderwildwood.ibasho.crypto

import org.junit.Assert
import org.junit.Test

class CryptoV2Test {
    @Test
    fun testToHexHash() {
        val actual = "FMD".toHash().toHexString()
        val expected = "b1277434dfe465a19f1db16df75f0cb28324b3692a852989c4dfeb865d347e85"
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testHkdfDerive() {
        val ikm = ByteArray(32) { 41 }
        val info = "fmd_v2_unit_test".toByteArray()
        val actual = CryptoV2.hkdfDerive(ikm, info).toHexString()

        val expected = "64394c58796450367852bd9a8effc2d1a76232958b0bca9ad4b59d15f33c38a8"
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun testHashPassword() {
        val salt = ByteArray(32) { 41 }
        val actual = CryptoV2.hashPassword("alice", "password", salt)

        val expectedAuthKey = "35fb781014180916aeaecb65e031b9af3cf5326caabaa7b72f619065c9e9e2ce"
        val expectedPmk = "d15f59206fc419ece0d86dd85fb8e4a99f9aeea7d6f9652378cea43f97767c10"

        Assert.assertEquals(salt, actual.salt)
        Assert.assertEquals(expectedAuthKey, actual.authKey.toHexString())
        Assert.assertEquals(expectedPmk, actual.preMasterKey.toHexString())
    }

    @Test
    fun testEncryptDecrypt() {
        for (type in DataBlobType.all()) {
            val masterKey = LongTermKeys.generate("alice")
            val expected = "foobar"

            val ciphertext = masterKey.encryptDataBlob(expected.toByteArray(), type)
            val plaintext = masterKey.decryptDataBlob(ciphertext)

            Assert.assertEquals(expected, plaintext!!.decodeToString())
        }
    }
}
