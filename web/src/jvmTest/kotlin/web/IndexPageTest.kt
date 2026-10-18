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

        assertEquals(listOf("styles.css", "web.js"), urls)
    }

    @Test
    fun `page loads the compiled game script`() {
        assertTrue("""<script src="web.js"></script>""" in page)
    }
}
