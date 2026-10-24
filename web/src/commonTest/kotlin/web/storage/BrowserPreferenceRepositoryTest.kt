package web.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BrowserPreferenceRepositoryTest {

    private val store = InMemoryKeyValueStore()
    private val repo = BrowserPreferenceRepository(store)

    @Test
    fun `a preference is one entry under mordovorot pref`() {
        repo.set("tile_labels", "letter_pairs")

        assertEquals(mapOf("mordovorot.pref.tile_labels" to "letter_pairs"), store.entries)
        assertEquals("letter_pairs", repo.get("tile_labels"))
    }

    @Test
    fun `nothing stored reads as null`() {
        assertNull(repo.get("tile_labels"))
    }

    @Test
    fun `a preference is never listed as a save`() {
        repo.set("tile_labels", "pairs")

        assertEquals(emptyList(), BrowserSaveRepository(store).listSaves())
    }
}
