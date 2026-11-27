package web

import texts.Language
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

    @Test
    fun `with an install entry the wide footer reads the credit, then Install the game`() {
        val text = footer("1.2.3", install = InstallMethod.BROWSER_PROMPT).joinToString("") { it.text }

        assertEquals("${page.footerVersion("1.2.3")}${page.footerCreditLead}Cary Huang (carykh) · ${page.footerOriginal} · loopover.xyz · ${page.footerInstall}", text)
    }

    @Test
    fun `with an install entry the narrow footer is the version and the entry, with no credit at all`() {
        val text = footer("1.2.3", install = InstallMethod.SAFARI_STEPS).filterNot { it.wideOnly }.joinToString("") { it.text }

        assertEquals("v1.2.3 · ${page.footerInstall}", text)
    }

    @Test
    fun `the install entry is a button part, and the credit parts, links too, are flagged for the one cut-off span`() {
        val parts = footer("1.2.3", install = InstallMethod.BROWSER_PROMPT)

        assertEquals(listOf(FooterPart.InstallLink(page.footerInstall)), parts.filterIsInstance<FooterPart.InstallLink>())
        assertEquals(parts.indexOfLast { it.credit }, parts.indexOfFirst { it is FooterPart.InstallLink } - 2, "the credit runs up to loopover.xyz, a separator before the entry")
        assertEquals(setOf(true), parts.filterIsInstance<FooterPart.Link>().map { it.credit }.toSet())
        assertEquals(false, parts.first().credit)
        assertEquals(false, parts.last().credit)
    }

    @Test
    fun `the new version notice wins over the install entry`() {
        val parts = footer("1.2.3", updateReady = true, install = InstallMethod.BROWSER_PROMPT)

        assertEquals(footer("1.2.3", updateReady = true), parts)
        assertEquals(emptyList(), parts.filterIsInstance<FooterPart.InstallLink>())
    }

    @Test
    fun `no install entry leaves the footer as it was, with no credit flag`() {
        assertEquals(emptyList(), footer("1.2.3").filter { it.credit })
    }

    @Test
    fun `the credit's version and lead still make up footerBasedOn in every language`() {
        for (language in Language.entries) {
            val page = language.texts.page
            assertEquals(page.footerVersion("1.2.3") + page.footerCreditLead, page.footerBasedOn("1.2.3"), language.code)
        }
    }
}
