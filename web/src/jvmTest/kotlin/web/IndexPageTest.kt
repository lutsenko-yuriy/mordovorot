package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-110: the tab title is `Mordovorot`, and the title appears nowhere else on the page. */
class IndexPageTest {
    private val page = File("src/jsMain/resources/index.html").readText()

    @Test
    fun `tab title is Mordovorot`() {
        assertEquals("Mordovorot", Regex("<title>(.*)</title>").find(page)?.groupValues?.get(1))
    }

    @Test
    fun `title appears nowhere else on the page`() {
        val body = page.substringAfter("<body").substringBefore("</body>")
        assertTrue("Mordovorot" !in body, "body mentions the title: $body")
    }

    @Test
    fun `page loads its files by relative URLs, so it works under the public site's sub-path`() {
        val urls = Regex("""(?:src|href)="([^"]*)"""").findAll(page).map { it.groupValues[1] }.toList()

        assertEquals(listOf("palette.css", "styles.css", "fit.css", "board.css", "dialogs.css", "solution.css", "web.js"), urls)
    }

    /** GH-153: a sheet left unlinked would ship in the bundle but never load. */
    @Test
    fun `page links every style sheet, and only sheets that exist`() {
        val linked = Regex("""<link rel="stylesheet" href="([^"]*)">""").findAll(page).map { it.groupValues[1] }.toSet()
        val sheets = File("src/jsMain/resources").listFiles { f -> f.extension == "css" }.orEmpty().map { it.name }.toSet()

        assertEquals(sheets, linked)
    }

    @Test
    fun `page loads the compiled game script`() {
        assertTrue("""<script src="web.js"></script>""" in page)
    }
}
