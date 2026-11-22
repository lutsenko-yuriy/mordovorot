package viewmodel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** GH-338: the autosave's slot names. */
class AutosaveSlotTest {

    @Test
    fun `a slot is named by the label scheme's wire name and the size`() {
        assertEquals("Autosave_numbers_4x4", AutosaveSlot.nameFor(TileLabels.NUMBERS, 4))
        assertEquals("Autosave_letter_pairs_9x9", AutosaveSlot.nameFor(TileLabels.LETTER_PAIRS, 9))
        assertEquals("Autosave_cyrillic_letter_pairs_9x9", AutosaveSlot.nameFor(TileLabels.CYRILLIC_LETTER_PAIRS, 9))
        assertEquals("Autosave_prereform_letters_6x6", AutosaveSlot.nameFor(TileLabels.PREREFORM_LETTERS, 6))
    }

    @Test
    fun `every scheme and size has its own slot, valid for the store`() {
        val names = TileLabels.entries.flatMap { labels -> (3..9).map { AutosaveSlot.nameFor(labels, it) } }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.all { name -> name.none { it == '/' || it == '\\' } })
    }

    @Test
    fun `a name is an autosave when it has the prefix`() {
        assertTrue(AutosaveSlot.isAutosave("Autosave_pairs_5x5"))
        assertFalse(AutosaveSlot.isAutosave("mine"))
        assertFalse(AutosaveSlot.isAutosave("autosave"))
    }
}
