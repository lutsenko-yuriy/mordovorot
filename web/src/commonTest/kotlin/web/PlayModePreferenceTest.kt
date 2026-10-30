package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import storage.PreferenceRepository
import web.storage.BrowserPreferenceRepository
import web.storage.InMemoryKeyValueStore

/** GH-171: the mode kept under `input_mode`; a failed read means none, a failed write is silent. */
class PlayModePreferenceTest {

    private object Broken : PreferenceRepository {
        override fun get(key: String): String? = throw IllegalStateException()
        override fun set(key: String, value: String) = throw IllegalStateException()
    }

    @Test
    fun `keeps the mode under input_mode by its wire name`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        val preference = PlayModePreference(store)
        assertNull(preference.load())

        assertTrue(preference.save(PlayMode.KEYBOARD))

        assertEquals("keyboard", store.get("input_mode"))
        assertEquals(PlayMode.KEYBOARD, preference.load())
    }

    @Test
    fun `an unknown value is no preference`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        store.set("input_mode", "rich_ui")
        assertNull(PlayModePreference(store).load())
    }

    @Test
    fun `a broken store means no preference, and a write just fails`() {
        assertNull(PlayModePreference(Broken).load())
        assertFalse(PlayModePreference(Broken).save(PlayMode.MOUSE))
    }
}
