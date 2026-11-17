package com.wanderwildwood.ibasho.locationproviders

import org.junit.Assert.assertEquals
import org.junit.Test

class WithoutKeyTest {
    @Test
    fun theKeyIsTakenOutOfTheAddress() {
        assertEquals(
            "https://opencellid.org/cell/get?mcc=310&mnc=260&lac=1&cellid=2&format=json",
            withoutKey("https://opencellid.org/cell/get?key=pk.secret123&mcc=310&mnc=260&lac=1&cellid=2&format=json"),
        )
        assertEquals("https://x.test/a?b=1", withoutKey("https://x.test/a?b=1&key=abc"))
        assertEquals("https://x.test/a", withoutKey("https://x.test/a?key=abc"))
    }
}
