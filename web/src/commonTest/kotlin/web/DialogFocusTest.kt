package web

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.TileLabels

/** GH-163: each dialog's control focused on open, and its title, the dialog's accessible name. */
class DialogFocusTest {
    private val labels = WebDialog.Labels(listOf(LabelOption(TileLabels.PAIRS, "Pairs", listOf("11"))), TileLabels.PAIRS)
    private val saves = listOf(SaveEntry("foo", "foo (4x4)"))

    @Test
    fun `each dialog focuses its default control`() {
        assertEquals(Focus.SizeChoice(5), defaultFocus(WebDialog.Size(listOf(3, 4, 5, 6), current = 5)))
        assertEquals(Focus.SaveName, defaultFocus(WebDialog.Save("foo", null)))
        assertEquals(Focus.FirstSave, defaultFocus(WebDialog.Load(restore = false, saves)))
        assertEquals(Focus.FirstSave, defaultFocus(WebDialog.Load(restore = true, saves)))
        assertEquals(Focus.DialogCancel, defaultFocus(WebDialog.Load(restore = false, emptyList())))
        assertEquals(Focus.Repair, defaultFocus(WebDialog.Unsolvable("x")))
        assertEquals(Focus.Label(TileLabels.PAIRS), defaultFocus(labels))
        assertEquals(Focus.No, defaultFocus(WebDialog.GiveUp))
    }

    @Test
    fun `each dialog is named by its title`() {
        assertEquals("New game size", WebDialog.Size(listOf(4), current = 4).title)
        assertEquals("Save game", WebDialog.Save("", null).title)
        assertEquals("Load game", WebDialog.Load(restore = false, saves).title)
        assertEquals("Restore a saved game?", WebDialog.Load(restore = true, saves).title)
        assertEquals("Can't be solved", WebDialog.Unsolvable("x").title)
        assertEquals("Tile labels", labels.title)
        assertEquals("Give up and see Mordie's solution?", WebDialog.GiveUp.title)
    }

    @Test
    fun `focus keys are distinct`() {
        val all = listOf(
            Focus.SizeChoice(3), Focus.SizeChoice(4), Focus.SaveName, Focus.FirstSave, Focus.DialogCancel, Focus.Repair,
            Focus.No, Focus.NewButton, Focus.SaveButton, Focus.LoadButton, Focus.LabelsButton, Focus.GiveUpRow,
            Focus.ReplayNext, Focus.ReplayPrev, Focus.ReplayRestart, Focus.PageStart,
        ) + TileLabels.entries.map { Focus.Label(it) } +
            Arrow.entries.flatMap { arrow -> (0 until 6).map { Focus.ArrowButton(arrow, it) } }
        assertEquals(all.size, all.map { it.key }.toSet().size)
    }
}
