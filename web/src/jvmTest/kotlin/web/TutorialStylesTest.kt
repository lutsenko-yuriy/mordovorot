package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-288: the tutorial's sheet is linked, its pulse is an animation that reduced motion stops, and its outline and bubble hold still. */
class TutorialStylesTest {
    private val css = File("src/jsMain/resources/tutorial.css").readText()

    private fun rule(selector: String) =
        checkNotNull(Regex("""(?m)^${Regex.escape(selector)} \{([^}]*)}""").find(css)?.groupValues?.get(1)) { "tutorial.css: no `$selector`" }

    @Test
    fun `index links tutorial css after keyboard css`() {
        val links = Regex("""href="([\w-]+\.css)"""").findAll(File("src/jsMain/resources/index.html").readText()).map { it.groupValues[1] }.toList()

        assertTrue(links.indexOf("tutorial.css") > links.indexOf("keyboard.css") && "keyboard.css" in links, links.toString())
    }

    @Test
    fun `the halo is an animation, which styles css stops under reduced motion`() {
        assertTrue("animation: asked-halo" in rule(".tutorial-line .mark"))
        assertTrue("@keyframes asked-halo" in css)
        assertTrue("animation" !in css.substringAfter("@keyframes asked-halo").substringAfter("\n}\n"))
    }

    @Test
    fun `the outline is solid colour under the halo`() {
        val mark = rule(".tutorial-line .mark")

        assertTrue("dashed var(--mordie)" in mark && "opacity" !in mark, mark)
        assertTrue("opacity" !in css.substringAfter("@keyframes asked-halo").substringBefore("\n}\n"))
    }

    @Test
    fun `the bubble keeps room for the longest text`() {
        assertTrue("min-height:" in rule(".bubble"))
    }
}
