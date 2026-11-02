package web.qr

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-243: `web.qr` imports the standard library only, so it can become a module of its own without edits. */
class QrBoundaryTest {

    @Test
    fun `web qr imports only kotlin`() {
        val files = File("src/commonMain/kotlin/web/qr").walkTopDown().filter { it.extension == "kt" }.toList()
        assertTrue(files.isNotEmpty(), "no web.qr sources found from ${File(".").absolutePath}")
        val imports = files.flatMap { file -> file.readLines().filter { it.startsWith("import ") && !it.startsWith("import kotlin.") }.map { "${file.name}: $it" } }
        assertEquals(emptyList(), imports)
    }
}
