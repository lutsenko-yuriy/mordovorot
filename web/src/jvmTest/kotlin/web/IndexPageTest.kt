package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-110: the tab title is `Mordovorot`, and the title appears nowhere else on the page. GH-166:
 *  the screen reader's live region. GH-168: Chrome on Android passes the system text size to the page. */
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

        assertEquals(listOf("manifest.webmanifest", "icons/favicon-32.png", "icons/icon-192.png", "icons/apple-touch-icon.png", "palette.css", "styles.css", "buttons.css", "fit.css", "board.css", "keyboard.css", "tutorial.css", "dialogs.css", "labels.css", "toggle.css", "language.css", "solution.css", "web.js"), urls)
    }

    /** GH-153: a sheet left unlinked would ship in the bundle but never load. */
    @Test
    fun `page links every style sheet, and only sheets that exist`() {
        val linked = Regex("""<link rel="stylesheet" href="([^"]*)">""").findAll(page).map { it.groupValues[1] }.toSet()
        val sheets = File("src/jsMain/resources").listFiles { f -> f.extension == "css" }.orEmpty().map { it.name }.toSet()

        assertEquals(sheets, linked)
    }

    /** GH-166: a live region must exist before its text changes, and outside #app, which every frame
     *  rebuilds and a dialog makes inert. */
    @Test
    fun `page has a polite live region next to the app`() {
        val body = page.substringAfter("<body>").substringBefore("<script")

        assertEquals(
            """<div id="app"></div>
<div id="announcer" class="visually-hidden" role="status" aria-live="polite" aria-atomic="true"></div>""",
            body.trim(),
        )
    }

    @Test
    fun `page loads the compiled game script`() {
        assertTrue("""<script src="web.js"></script>""" in page)
    }

    /** GH-168: without it, Chrome on Android leaves the default font size at 16px whatever the system's. */
    @Test
    fun `page asks for the system text size`() {
        assertTrue("""<meta name="text-scale" content="scale">""" in page.substringBefore("</head>"))
    }

    private val head = page.substringBefore("</head>")

    /** GH-357: the manifest, its icons and the page colours make the game installable. */
    @Test
    fun `page links the manifest and the icons`() {
        assertTrue("""<link rel="manifest" href="manifest.webmanifest">""" in head)
        assertTrue("""<link rel="icon" href="icons/favicon-32.png" sizes="32x32" type="image/png">""" in head)
        assertTrue("""<link rel="apple-touch-icon" href="icons/apple-touch-icon.png">""" in head)
    }

    @Test
    fun `every file the head links exists`() {
        Regex("""<link rel="(?:manifest|icon|apple-touch-icon)" href="([^"]*)"""").findAll(head).forEach {
            assertTrue(File("src/jsMain/resources/${it.groupValues[1]}").isFile, "${it.groupValues[1]} is not under src/jsMain/resources")
        }
    }

    /** The bar and title colours follow the page's own, in each theme. */
    @Test
    fun `theme colour is the page colour in light and in dark`() {
        val light = Palette.light.getValue("page")
        val dark = Palette.dark.getValue("page")

        assertTrue("""<meta name="theme-color" content="$light" media="(prefers-color-scheme: light)">""" in head)
        assertTrue("""<meta name="theme-color" content="$dark" media="(prefers-color-scheme: dark)">""" in head)
    }

    /** Safari's own web-app tags: the title under the icon, and a status bar that keeps the page below it. */
    @Test
    fun `page asks to open as an app on iOS and Android`() {
        listOf(
            """<meta name="mobile-web-app-capable" content="yes">""",
            """<meta name="apple-mobile-web-app-capable" content="yes">""",
            """<meta name="apple-mobile-web-app-title" content="Mordovorot">""",
            """<meta name="apple-mobile-web-app-status-bar-style" content="default">""",
        ).forEach { assertTrue(it in head, "missing $it") }
    }

    /** Without `viewport-fit=cover` the browser keeps the page inside the safe area, so no insets are needed. */
    @Test
    fun `viewport does not draw under the notch`() {
        assertTrue("viewport-fit" !in head)
    }
}
