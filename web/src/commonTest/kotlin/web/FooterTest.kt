package web

import texts.texts
import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-111 scenarios: the footer's wording and links. */
class FooterTest {
    private val page = texts().page


    @Test
    fun `footer reads version and Loopover credit`() {
        val text = footer("1.2.3").joinToString("") { it.text }

        assertEquals("${page.footerBasedOn("1.2.3")}Cary Huang (carykh) · ${page.footerOriginal} · loopover.xyz", text)
    }

    @Test
    fun `footer links point to the original and loopover xyz`() {
        val links = footer("1.2.3").filterIsInstance<FooterPart.Link>()

        assertEquals(
            listOf(
                FooterPart.Link(page.footerOriginal, "https://openprocessing.org/@carykh/580366", wideOnly = true),
                FooterPart.Link("loopover.xyz", "https://loopover.xyz", wideOnly = true),
            ),
            links,
        )
    }

    @Test
    fun `footer announces a new version with an Update link`() {
        val parts = footer("1.2.3", updateReady = true)

        assertEquals(page.footerUpdateNotice("1.2.3") + page.footerUpdate, parts.joinToString("") { it.text })
        assertEquals(listOf(FooterPart.UpdateLink(page.footerUpdate)), parts.filterIsInstance<FooterPart.UpdateLink>())
    }

    @Test
    fun `the new version notice has no wide-only parts, so the narrow footer reads the same`() {
        val parts = footer("1.2.3", updateReady = true)

        assertEquals(parts.joinToString("") { it.text }, parts.filterNot { it.wideOnly }.joinToString("") { it.text })
    }

    @Test
    fun `the worker is registered by a relative URL, as the public site's sub-path requires`() {
        assertEquals("offline-worker.js", OFFLINE_WORKER)
    }

    @Test
    fun `narrow footer keeps the version and the short credit, without links`() {
        val text = footer("1.2.3").filterNot { it.wideOnly }.joinToString("") { it.text }

        assertEquals(page.footerBasedOn("1.2.3") + "carykh", text)
    }
}
