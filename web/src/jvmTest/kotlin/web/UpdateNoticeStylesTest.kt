package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-262: the footer's Update is a button that looks like a link. GH-360: the Install entry shares its rules. */
class UpdateNoticeStylesTest {
    private val sheet = File("src/jsMain/resources/buttons.css").readText()
    private val update = Regex("""\.footer \.update,\s*\.footer \.install \{([^}]*)}""").find(sheet)?.groupValues?.get(1).orEmpty()

    @Test
    fun `Update and Install are bold and underlined`() {
        assertTrue("font-weight: 700;" in update && "text-decoration: underline;" in update, update)
    }

    @Test
    fun `Update and Install have no button chrome and take the surrounding font`() {
        assertTrue("border: 0;" in update && "background: none;" in update && "font: inherit;" in update, update)
    }

    @Test
    fun `Update and Install keep a ring for keyboard focus`() {
        assertTrue(Regex("""\.footer \.update:focus-visible,\s*\.footer \.install:focus-visible \{[^}]*outline: 3px solid""").containsMatchIn(sheet), sheet)
    }
}
