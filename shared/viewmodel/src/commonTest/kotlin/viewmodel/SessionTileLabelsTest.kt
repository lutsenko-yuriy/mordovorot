package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import testing.FakePreferenceRepository

class SessionTileLabelsTest {

    private val store = FakePreferenceRepository()

    private fun session(stored: String? = null): SessionTileLabels {
        stored?.let { store.values["tile_labels"] = it }
        return SessionTileLabels(TileLabelsPreference(store))
    }

    @Test
    fun `with no preference the labels are the default numbers`() {
        val labels = session()
        assertEquals(TileLabels.NUMBERS, labels.current)
        assertEquals(TileLabelsSource.DEFAULT, labels.source)
        assertNull(labels.preferred)
        assertEquals(TileLabels.NUMBERS, labels.effectivePreference)
    }

    @Test
    fun `a stored preference wins over the default`() {
        val labels = session("pairs")
        assertEquals(TileLabels.PAIRS, labels.current)
        assertEquals(TileLabelsSource.PREFERENCE, labels.source)
        assertEquals(TileLabels.PAIRS, labels.effectivePreference)
    }

    @Test
    fun `a save's labels win over the preference`() {
        val labels = session("pairs")
        labels.applySave(TileLabels.LETTERS, 4)
        assertEquals(TileLabels.LETTERS, labels.current)
        assertEquals(TileLabelsSource.SAVE, labels.source)
        assertEquals(TileLabels.PAIRS, labels.effectivePreference)
    }

    @Test
    fun `a save with no labels falls back to the preference, or the default`() {
        val withPreference = session("pairs").apply { applySave(TileLabels.LETTERS, 4); applySave(null, 4) }
        assertEquals(TileLabels.PAIRS, withPreference.current)
        assertEquals(TileLabelsSource.PREFERENCE, withPreference.source)

        store.values.clear()
        val withoutPreference = session().apply { applySave(TileLabels.LETTERS, 4); applySave(null, 4) }
        assertEquals(TileLabels.NUMBERS, withoutPreference.current)
        assertEquals(TileLabelsSource.DEFAULT, withoutPreference.source)
    }

    @Test
    fun `a new game goes back to the preference`() {
        val labels = session("pairs")
        labels.applySave(TileLabels.LETTERS, 4)
        labels.resetToPreference(4)
        assertEquals(TileLabels.PAIRS, labels.current)
        assertEquals(TileLabelsSource.PREFERENCE, labels.source)
    }

    @Test
    fun `choosing a scheme relabels, becomes the preference and is stored`() {
        val labels = session()
        assertTrue(labels.choose(TileLabels.LETTER_PAIRS))
        assertEquals(TileLabels.LETTER_PAIRS, labels.current)
        assertEquals(TileLabelsSource.PREFERENCE, labels.source)
        assertEquals(TileLabels.LETTER_PAIRS, labels.preferred)
        assertEquals("letter_pairs", store.values["tile_labels"])
    }

    @Test
    fun `choosing the scheme already shown changes nothing and writes nothing`() {
        val labels = session("pairs")
        labels.applySave(TileLabels.LETTERS, 4)
        assertFalse(labels.choose(TileLabels.LETTERS))
        assertEquals(TileLabelsSource.SAVE, labels.source)
        assertEquals(TileLabels.PAIRS, labels.preferred)
        assertEquals("pairs", store.values["tile_labels"])
    }

    @Test
    fun `a failed preference write still relabels`() {
        val labels = session()
        store.setException = RuntimeException("read-only")
        assertTrue(labels.choose(TileLabels.PAIRS))
        assertEquals(TileLabels.PAIRS, labels.current)
        labels.resetToPreference(4)
        assertEquals(TileLabels.PAIRS, labels.current)
    }

    // --- 6x6, GH-160 ---

    @Test
    fun `a Letters preference shows numbers on 6x6 and keeps the preference`() {
        val labels = session("letters")
        labels.resetToPreference(6)
        assertEquals(TileLabels.NUMBERS, labels.current)
        assertEquals(TileLabelsSource.LETTERS_UNAVAILABLE, labels.source)
        assertEquals(TileLabels.LETTERS, labels.preferred)

        labels.resetToPreference(5)
        assertEquals(TileLabels.LETTERS, labels.current)
        assertEquals(TileLabelsSource.PREFERENCE, labels.source)
    }

    @Test
    fun `a save's Letters show numbers on 6x6`() {
        val labels = session("pairs")
        labels.applySave(TileLabels.LETTERS, 6)
        assertEquals(TileLabels.NUMBERS, labels.current)
        assertEquals(TileLabelsSource.LETTERS_UNAVAILABLE, labels.source)
        assertEquals(TileLabels.PAIRS, labels.preferred)
    }

    @Test
    fun `no save labels and a Letters preference show numbers on 6x6`() {
        val labels = session("letters")
        labels.applySave(null, 6)
        assertEquals(TileLabels.NUMBERS, labels.current)
        assertEquals(TileLabelsSource.LETTERS_UNAVAILABLE, labels.source)
    }

    @Test
    fun `OK on the numbers standing in for Letters changes nothing`() {
        val labels = session("letters")
        labels.resetToPreference(6)
        assertFalse(labels.choose(TileLabels.NUMBERS))
        assertEquals(TileLabels.LETTERS, labels.preferred)
        assertEquals("letters", store.values["tile_labels"])
    }
}
