package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-357: the web app manifest makes the game installable. It is read as text, like the style
 *  sheets are; the build's `checkWebManifest` parses it as JSON. Every URL is relative, since the
 *  public site is under a sub-path (GH-111) and the dev server is at the root. */
class WebManifestTest {
    private val resources = File("src/jsMain/resources")
    private val manifest = File(resources, "manifest.webmanifest").readText()
    private val palette = File(resources, "palette.css").readText().substringBefore("@media")

    private fun string(member: String) = Regex(""""$member"\s*:\s*"([^"]*)"""").find(manifest)?.groupValues?.get(1)

    private class Icon(val src: String, val sizes: String, val type: String, val purpose: String)

    private val icons = Regex("""\{[^{}]*"src"[^{}]*}""").findAll(manifest.substringAfter(""""icons"""")).map { block ->
        fun member(name: String) = Regex(""""$name"\s*:\s*"([^"]*)"""").find(block.value)?.groupValues?.get(1) ?: ""
        Icon(member("src"), member("sizes"), member("type"), member("purpose"))
    }.toList()

    @Test
    fun `required members are present`() {
        listOf("id", "name", "short_name", "start_url", "scope", "display", "theme_color", "background_color").forEach {
            assertTrue(string(it) != null, "manifest has no $it")
        }
        assertTrue(icons.isNotEmpty(), "manifest has no icons")
    }

    @Test
    fun `one name in every language`() {
        assertEquals("Mordovorot", string("name"))
        assertEquals("Mordovorot", string("short_name"))
    }

    @Test
    fun `the installed game opens in its own window, in any orientation`() {
        assertEquals("standalone", string("display"))
        assertTrue("orientation" !in manifest, "orientation is left free")
    }

    @Test
    fun `identity, start page and scope are the page's own folder, and relative`() {
        assertEquals("./", string("id"))
        assertEquals("./", string("start_url"))
        assertEquals("./", string("scope"))
    }

    @Test
    fun `every icon url is relative, and a file that ships`() {
        icons.forEach { icon ->
            assertTrue(!icon.src.startsWith("/") && "://" !in icon.src, "${icon.src} is not relative")
            assertTrue(File(resources, icon.src).isFile, "${icon.src} is not under src/jsMain/resources")
        }
    }

    @Test
    fun `colours are the light page colour`() {
        val page = Regex("""--page:\s*([^;]+);""").find(palette)!!.groupValues[1].trim()

        assertEquals(page, string("theme_color"))
        assertEquals(page, string("background_color"))
    }

    @Test
    fun `icons cover 192 and 512 and a maskable 512`() {
        assertTrue(icons.any { it.sizes == "192x192" && it.purpose == "any" }, "no 192 icon")
        assertTrue(icons.any { it.sizes == "512x512" && it.purpose == "any" }, "no 512 icon")
        assertTrue(icons.any { it.sizes == "512x512" && it.purpose == "maskable" }, "no maskable 512 icon")
        icons.forEach { assertEquals("image/png", it.type, it.src) }
    }
}
