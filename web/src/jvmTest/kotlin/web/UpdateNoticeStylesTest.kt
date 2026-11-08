package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-262: the footer's Update is a button that looks like a link. */
class UpdateNoticeStylesTest {
    private val sheet = File("src/jsMain/resources/buttons.css").readText()
    private val update = Regex("""\.footer \.update \{([^}]*)}""").find(sheet)?.groupValues?.get(1).orEmpty()

    @Test
    fun `Update is bold and underlined`() {
        assertTrue("font-weight: 700;" in update && "text-decoration: underline;" in update, update)
    }

    @Test
    fun `Update has no button chrome and takes the surrounding font`() {
        assertTrue("border: 0;" in update && "background: none;" in update && "font: inherit;" in update, update)
    }

    @Test
    fun `Update keeps a ring for keyboard focus`() {
        assertTrue(Regex("""\.footer \.update:focus-visible \{[^}]*outline: 3px solid""").containsMatchIn(sheet), sheet)
    }
}
