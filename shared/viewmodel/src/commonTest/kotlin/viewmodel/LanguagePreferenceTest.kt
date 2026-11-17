package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.FakePreferenceRepository
import texts.Language

class LanguagePreferenceTest {

    private val store = FakePreferenceRepository()
    private val preference = LanguagePreference(store)

    @Test
    fun `nothing stored gives null`() {
        assertNull(preference.load())
    }

    @Test
    fun `a stored language loads by its code`() {
        store.values["language"] = "ru"
        assertEquals(Language.RU, preference.load())
    }

    @Test
    fun `an unknown code gives null`() {
        store.values["language"] = "xx"
        assertNull(preference.load())
    }

    @Test
    fun `a failed read gives null`() {
        store.getException = RuntimeException("unreadable")
        assertNull(preference.load())
    }

    @Test
    fun `save stores the code under language`() {
        assertTrue(preference.save(Language.DE))
        assertEquals(mapOf("language" to "de"), store.values)
    }

    @Test
    fun `a failed write gives false`() {
        store.setException = RuntimeException("read-only")
        assertFalse(preference.save(Language.DE))
    }
}
