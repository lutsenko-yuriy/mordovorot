package texts

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class CurrentTextsTest {

    @AfterTest
    fun backToEnglish() = useLanguage(Language.EN)

    @Test
    fun `English is the language until another is used`() {
        assertEquals(Language.EN, activeLanguage())
        assertSame(Language.EN.texts, texts())
    }

    @Test
    fun `using a language is what texts returns`() {
        Language.entries.forEach {
            useLanguage(it)
            assertEquals(it, activeLanguage())
            assertSame(it.texts, texts())
        }
    }
}
