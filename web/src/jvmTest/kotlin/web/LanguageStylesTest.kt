package web

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/** GH-275: the language picker shows the toggles on wide windows and the dropdown on phones, never both. */
class LanguageStylesTest {
    private val sheet = File("src/jsMain/resources/language.css").readText()
    private val phones = "@media (max-width: 480px), (orientation: landscape) and (max-width: 760px) {"

    @Test
    fun `the dropdown is hidden until the phone breakpoint, and the toggles then give way`() {
        assertTrue(Regex("""\.language-dropdown \{[^}]*display: none;""").containsMatchIn(sheet), sheet)
        val phone = sheet.substringAfter(phones)
        assertTrue(".language-toggles {\n        display: none;" in phone && ".language-dropdown {\n        display: block;" in phone, phone)
    }

    @Test
    fun `the picker keeps a ring for keyboard focus`() {
        assertTrue(Regex("""\.language-toggle:focus-visible,\s*\.language-dropdown:focus-within \{[^}]*outline: 3px solid""").containsMatchIn(sheet), sheet)
    }

    @Test
    fun `the native select is transparent over the visible code`() {
        assertTrue(Regex("""\.language-dropdown select \{[^}]*opacity: 0;""").containsMatchIn(sheet), sheet)
    }
}
