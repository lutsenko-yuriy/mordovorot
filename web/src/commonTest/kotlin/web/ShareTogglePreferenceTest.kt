package web

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import storage.PreferenceRepository
import web.storage.BrowserPreferenceRepository
import web.storage.InMemoryKeyValueStore

/** GH-267, GH-272: a Share dialog toggle kept under its key (`share_labels`, `share_progress`); off when nothing readable is stored. */
class ShareTogglePreferenceTest {

    private object Broken : PreferenceRepository {
        override fun get(key: String): String? = throw IllegalStateException()
        override fun set(key: String, value: String) = throw IllegalStateException()
    }

    @Test
    fun `is off until turned on, and keeps its state as on or off`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        val preference = ShareTogglePreference(store, "share_labels")
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
        assertFalse(ShareTogglePreference(store, "share_labels").load())
    }

    @Test
    fun `a broken store means off, and a write just fails`() {
        assertFalse(ShareTogglePreference(Broken, "share_labels").load())
        assertFalse(ShareTogglePreference(Broken, "share_labels").save(true))
    }

    @Test
    fun `each key keeps its own state`() {
        val store = BrowserPreferenceRepository(InMemoryKeyValueStore())
        val labels = ShareTogglePreference(store, "share_labels")
        val progress = ShareTogglePreference(store, "share_progress")

        assertTrue(progress.save(true))

        assertEquals("on", store.get("share_progress"))
        assertTrue(progress.load())
        assertFalse(labels.load())
        assertNull(store.get("share_labels"))
    }
}
