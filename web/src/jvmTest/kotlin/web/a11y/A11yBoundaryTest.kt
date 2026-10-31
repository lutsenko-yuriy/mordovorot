package web.a11y

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-201: `web` depends on `web.a11y`, never the other way, until a Gradle module enforces it. */
class A11yBoundaryTest {
    private val a11yFiles = listOf("commonMain", "jsMain").flatMap { source ->
        File("src/$source/kotlin/web/a11y").walkTopDown().filter { it.extension == "kt" }.toList()
    }

    @Test
    fun `web a11y imports no other package of web`() {
        assertTrue(a11yFiles.isNotEmpty(), "no web.a11y sources found from ${File(".").absolutePath}")
        val imports = a11yFiles.flatMap { file ->
            file.readLines().filter { it.startsWith("import web.") && !it.startsWith("import web.a11y.") }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList(), imports)
    }
}
