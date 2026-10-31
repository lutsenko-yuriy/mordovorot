package web.a11y

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-201: `web` depends on `web.a11y`, never the other way, until a Gradle module enforces it. */
class A11yBoundaryTest {
    private val a11yFiles = listOf("commonMain", "jsMain").flatMap { source ->
        File("src/$source/kotlin/web/a11y").walkTopDown().filter { it.extension == "kt" }.toList()
    }

    @Test
    fun `web a11y imports no other package of web`() {
        val imports = a11yFiles.flatMap { file ->
            file.readLines().filter { it.startsWith("import web.") && !it.startsWith("import web.a11y.") }.map { "${file.name}: $it" }
        }
        assertEquals(emptyList(), imports)
    }
}
