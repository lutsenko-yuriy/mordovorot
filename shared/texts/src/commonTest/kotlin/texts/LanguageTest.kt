package texts

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LanguageTest {

    @Test
    fun `a code finds its language`() {
        assertEquals(Language.RU, Language.fromCode("ru"))
        assertEquals(Language.EN, Language.fromCode("en"))
        assertEquals(Language.DE, Language.fromCode("de"))
    }

    @Test
    fun `an unknown code finds none`() {
        assertNull(Language.fromCode("fr"))
        assertNull(Language.fromCode("RU"))
        assertNull(Language.fromCode(null))
    }

    @Test
    fun `codes and native names are unique`() {
        assertEquals(Language.entries.size, Language.entries.map { it.code }.toSet().size)
        assertEquals(Language.entries.size, Language.entries.map { it.nativeName }.toSet().size)
    }

    @Test
    fun `the browser's first language picks by its primary subtag`() {
        assertEquals(Language.DE, Language.fromBrowser(listOf("de-AT")))
        assertEquals(Language.RU, Language.fromBrowser(listOf("RU")))
        assertEquals(Language.EN, Language.fromBrowser(listOf("en-GB", "de")))
        assertEquals(Language.DE, Language.fromBrowser(listOf("de_CH")))
    }

    @Test
    fun `only the browser's first language counts`() {
        assertNull(Language.fromBrowser(listOf("uk", "ru")))
        assertNull(Language.fromBrowser(listOf("fr", "de")))
    }

    @Test
    fun `nothing usable in the browser's languages gives none`() {
        assertNull(Language.fromBrowser(emptyList()))
        assertNull(Language.fromBrowser(listOf("")))
        assertNull(Language.fromBrowser(listOf("*")))
    }
}
