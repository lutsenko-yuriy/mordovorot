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
}
