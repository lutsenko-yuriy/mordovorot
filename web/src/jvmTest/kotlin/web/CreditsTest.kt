package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-111: the footer's credit and the README's Credits section name the same creator and links. */
class CreditsTest {
    private val credits = File("../README.md").readText().substringAfter("## Credits").substringBefore("\n## ")

    @Test
    fun `README credits name the footer's creator and links`() {
        for (expected in listOf("Cary Huang (carykh)", ORIGINAL_URL, LOOPOVER_URL)) {
            assertTrue(expected in credits, "README's Credits lack $expected: $credits")
        }
    }
}
