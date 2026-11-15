package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import storage.PreferenceRepository
import web.storage.BrowserPreferenceRepository
import web.storage.InMemoryKeyValueStore

/** GH-288: the tutorial is unseen until marked, and a broken store counts as seen. */
class TutorialPreferenceTest {

    private object Broken : PreferenceRepository {
        override fun get(key: String): String? = throw IllegalStateException()
        override fun set(key: String, value: String) = throw IllegalStateException()
    }

    @Test
    fun `is unseen until marked, then keeps seen`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        val preference = TutorialPreference(store)
        assertFalse(preference.seen())

        assertTrue(preference.markSeen())

        assertEquals("seen", store.get("tutorial"))
        assertTrue(TutorialPreference(store).seen())
    }

    @Test
    fun `an unknown value is unseen`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        store.set("tutorial", "yes")

        assertFalse(TutorialPreference(store).seen())
    }

    @Test
    fun `a failed read counts as seen`() {
        assertTrue(TutorialPreference(Broken).seen())
    }

    @Test
    fun `a failed write is reported`() {
        assertFalse(TutorialPreference(Broken).markSeen())
    }
}
