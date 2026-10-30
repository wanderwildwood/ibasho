package com.wanderwildwood.ibasho.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PrivacyUrlTest {
    @Test fun `https server gets its privacy page`() =
        assertEquals("https://fmd.example.org/privacy?embedded=true", privacyPolicyUrl("https://fmd.example.org/"))

    @Test fun `http and empty addresses are refused`() {
        assertNull(privacyPolicyUrl("http://fmd.example.org"))
        assertNull(privacyPolicyUrl(""))
        assertNull(privacyPolicyUrl("https://"))
    }
}
