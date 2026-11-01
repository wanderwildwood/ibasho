package com.wanderwildwood.ibasho.ui

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalTextTest {
    private val gpl = File("src/main/res/raw/licence_gpl3.txt")

    @Test
    fun theGplTextIsBundledWhole() {
        val text = gpl.readText()
        assertTrue(text.contains("GNU GENERAL PUBLIC LICENSE"))
        assertTrue(text.contains("END OF TERMS AND CONDITIONS"))
        assertEquals(File("../LICENSE").readText(), text)
    }

    @Test
    fun paragraphsJoinWrappedLinesAndSplitOnBlankLines() {
        val p = licenceParagraphs("one\ntwo\n\n  three \n\n\nfour")
        assertEquals(listOf("one two", "three", "four"), p)
        assertTrue(licenceParagraphs(gpl.readText()).size > 50)
    }

    @Test
    fun sourceLinkIsTheAppsPublicRepo() {
        assertEquals("https://github.com/wanderwildwood/ibasho", SOURCE_URL)
    }
}
