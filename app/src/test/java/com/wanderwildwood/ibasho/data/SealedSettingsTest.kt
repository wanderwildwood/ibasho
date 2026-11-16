package com.wanderwildwood.ibasho.data

import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V1_HASHED_PASSWORD
import com.wanderwildwood.ibasho.data.EncryptedSettingsRepository.Companion.KEY_FMDSERVER_V1_PRIVATE_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class SealedSettingsTest {

    private fun plain(): HashMap<Int, Any?> = hashMapOf(
        Settings.SET_FMDSERVER_ID to "ada-whitlock",
        Settings.SET_FMD_CRYPT_HPW to "made-up-login-hash",
        Settings.SET_FMD_CRYPT_PRIVKEY to "made-up-wrapped-key",
        Settings.SET_FMD_CRYPT_PUBKEY to "made-up-public-key",
    )

    @Test
    fun takeOutMovesBothAndLeavesNoPlainCopy() {
        val settings = plain()
        val taken = SealedSettings.takeOut(settings)
        assertEquals(
            mapOf(
                KEY_FMDSERVER_V1_HASHED_PASSWORD to "made-up-login-hash",
                KEY_FMDSERVER_V1_PRIVATE_KEY to "made-up-wrapped-key",
            ),
            taken,
        )
        assertFalse(settings.containsKey(Settings.SET_FMD_CRYPT_HPW))
        assertFalse(settings.containsKey(Settings.SET_FMD_CRYPT_PRIVKEY))
        // Everything else stays where it was, the public key included.
        assertEquals("ada-whitlock", settings[Settings.SET_FMDSERVER_ID])
        assertEquals("made-up-public-key", settings[Settings.SET_FMD_CRYPT_PUBKEY])
    }

    @Test
    fun blankValuesAreRemovedButNotMoved() {
        val settings: HashMap<Int, Any?> = hashMapOf(
            Settings.SET_FMD_CRYPT_HPW to "",
            Settings.SET_FMD_CRYPT_PRIVKEY to "",
        )
        assertTrue(SealedSettings.takeOut(settings).isEmpty())
        assertTrue(settings.isEmpty())
    }

    @Test
    fun nothingToMove() {
        val settings: HashMap<Int, Any?> = hashMapOf(Settings.SET_FMDSERVER_ID to "ada-whitlock")
        assertTrue(SealedSettings.takeOut(settings).isEmpty())
        assertEquals(1, settings.size)
    }

    @Test
    fun putBackForAnExportRoundTrips() {
        val store = SealedSettings.takeOut(plain())
        val exported: HashMap<Int, Any?> = hashMapOf(Settings.SET_FMDSERVER_ID to "ada-whitlock")
        SealedSettings.putBack(exported) { store[it] ?: "" }
        assertEquals(plain().filterKeys { it != Settings.SET_FMD_CRYPT_PUBKEY }, exported)
    }

    @Test
    fun putBackLeavesOutWhatIsNotSet() {
        val exported: HashMap<Int, Any?> = hashMapOf()
        SealedSettings.putBack(exported) { "" }
        assertTrue(exported.isEmpty())
    }

    @Test
    fun onlyTheV1LoginIsSealed() {
        assertTrue(SealedSettings.isSealed(Settings.SET_FMD_CRYPT_HPW))
        assertTrue(SealedSettings.isSealed(Settings.SET_FMD_CRYPT_PRIVKEY))
        assertFalse(SealedSettings.isSealed(Settings.SET_FMD_CRYPT_PUBKEY))
    }
}
