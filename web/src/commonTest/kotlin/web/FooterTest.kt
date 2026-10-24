package web

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-111 scenarios: the footer's wording and links. */
class FooterTest {

    @Test
    fun `footer reads version and Loopover credit`() {
        val text = footer("1.2.3").joinToString("") { it.text }

        assertEquals("v1.2.3 · Based on Loopover by Cary Huang (carykh) · original · loopover.xyz", text)
    }

    @Test
    fun `footer links point to the original and loopover xyz`() {
        val links = footer("1.2.3").filterIsInstance<FooterPart.Link>()

        assertEquals(
            listOf(
                FooterPart.Link("original", "https://openprocessing.org/@carykh/580366", wideOnly = true),
                FooterPart.Link("loopover.xyz", "https://loopover.xyz", wideOnly = true),
            ),
            links,
        )
    }

    @Test
    fun `narrow footer keeps the version and the short credit, without links`() {
        val text = footer("1.2.3").filterNot { it.wideOnly }.joinToString("") { it.text }

        assertEquals("v1.2.3 · Based on Loopover by carykh", text)
    }
}
