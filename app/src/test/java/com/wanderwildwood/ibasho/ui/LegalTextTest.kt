package com.wanderwildwood.ibasho.ui

import com.wanderwildwood.ibasho.ui.settings.SOURCE_URL
import com.wanderwildwood.ibasho.ui.settings.licenceParagraphs
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegalTextTest {
    private val gpl = File("src/main/res/raw/gpl_3_0.txt")

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
