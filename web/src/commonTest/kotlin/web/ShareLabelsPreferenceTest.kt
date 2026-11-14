package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import storage.PreferenceRepository
import web.storage.BrowserPreferenceRepository
import web.storage.InMemoryKeyValueStore

/** GH-267: the Share dialog's toggle kept under `share_labels`; off when nothing readable is stored. */
class ShareLabelsPreferenceTest {

    private object Broken : PreferenceRepository {
        override fun get(key: String): String? = throw IllegalStateException()
        override fun set(key: String, value: String) = throw IllegalStateException()
    }

    @Test
    fun `is off until turned on, and keeps its state as on or off`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        val preference = ShareLabelsPreference(store)
        assertFalse(preference.load())

        assertTrue(preference.save(true))
        assertEquals("on", store.get("share_labels"))
        assertTrue(preference.load())

        assertTrue(preference.save(false))
        assertEquals("off", store.get("share_labels"))
        assertFalse(preference.load())
    }

    @Test
    fun `an unknown value is off`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        store.set("share_labels", "yes")
        assertFalse(ShareLabelsPreference(store).load())
    }

    @Test
    fun `a broken store means off, and a write just fails`() {
        assertFalse(ShareLabelsPreference(Broken).load())
        assertFalse(ShareLabelsPreference(Broken).save(true))
    }
}
