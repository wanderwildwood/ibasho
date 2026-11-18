package com.wanderwildwood.ibasho.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import javax.crypto.AEADBadTagException


class EncryptedSettingsKeyLostTest {

    private fun lost(e: Throwable) = EncryptedSettingsRepository.meansKeyLost(e)

    @Test
    fun aBusyKeystoreIsNotALostKey() {
        // What keystore2 throws when a cipher cannot start for now.
        val busy = InvalidKeyException("Keystore operation failed", RuntimeException("busy"))
        assertFalse(lost(busy))
        assertFalse(lost(RuntimeException("could not open", busy)))
    }

    @Test
    fun tinksFailedDecryptIsNotALostKey() {
        assertFalse(lost(GeneralSecurityException("decryption failed")))
        assertFalse(lost(GeneralSecurityException("decryption failed", InvalidKeyException("x"))))
    }

    @Test
    fun aTagThatNoLongerMatchesIsALostKey() {
        assertTrue(lost(AEADBadTagException()))
        assertTrue(lost(GeneralSecurityException("decryption failed", AEADBadTagException())))
    }

    @Test
    fun aCauseThatPointsAtItselfEnds() {
        val e = object : Exception("loop") {
            override val cause: Throwable get() = this
        }
        assertFalse(lost(e))
    }
}
