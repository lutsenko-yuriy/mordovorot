package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.FakePreferenceRepository

class TileLabelsPreferenceTest {

    private val store = FakePreferenceRepository()
    private val preference = TileLabelsPreference(store)

    @Test
    fun `nothing stored gives null`() {
        assertNull(preference.load())
    }

    @Test
    fun `a stored scheme loads by its wire name`() {
        store.values["tile_labels"] = "letter_pairs"
        assertEquals(TileLabels.LETTER_PAIRS, preference.load())
    }

    @Test
    fun `an unknown name gives null`() {
        store.values["tile_labels"] = "emoji"
        assertNull(preference.load())
    }

    @Test
    fun `a failed read gives null`() {
        store.getException = RuntimeException("unreadable")
        assertNull(preference.load())
    }

    @Test
    fun `save stores the wire name under tile_labels`() {
        assertTrue(preference.save(TileLabels.PAIRS_FROM_0))
        assertEquals(mapOf("tile_labels" to "pairs_from_0"), store.values)
    }

    @Test
    fun `a failed write gives false`() {
        store.setException = RuntimeException("read-only")
        assertFalse(preference.save(TileLabels.PAIRS))
    }
}
