package web

import kotlin.test.Test
import kotlin.test.assertEquals
import viewmodel.GiveUpButton
import viewmodel.SolutionState
import viewmodel.TileSelection

/** GH-164: where focus goes when the control that had it is gone or disabled after a redraw. */
class PageFocusTest {
    private val playing = WebState(side = 3, tiles = (1..9).toList(), moves = 4, mordie = null, solved = false, status = null, dialog = null)
    private val won = playing.copy(solved = true)
    private fun replay(atStart: Boolean, atEnd: Boolean) =
        playing.copy(solution = SolutionState(listOf("01. Row 1 ← ×1", "02. Col 2 ↑ ×1"), current = null, atStart = atStart, atEnd = atEnd, moves = emptyList()))

    @Test
    fun `a winning arrow passes focus to Mordie's solution when it is shown`() {
        val key = Focus.ArrowButton(Arrow.UP, 0).key
        assertEquals(Focus.GiveUpRow, fallbackFocus(key, won.copy(giveUp = GiveUpButton.MORDIES_SOLUTION)))
    }

    @Test
    fun `a winning arrow passes focus to New when Mordie's solution isn't shown`() {
        val key = Focus.ArrowButton(Arrow.LEFT, 2).key
        assertEquals(Focus.NewButton, fallbackFocus(key, won.copy(giveUp = GiveUpButton.HIDDEN)))
    }

    @Test
    fun `Read board on a board that got solved passes focus as a winning arrow does`() {
        assertEquals(Focus.GiveUpRow, fallbackFocus(Focus.ReadBoard.key, won.copy(giveUp = GiveUpButton.MORDIES_SOLUTION)))
        assertEquals(Focus.NewButton, fallbackFocus(Focus.ReadBoard.key, won.copy(giveUp = GiveUpButton.HIDDEN)))
    }

    @Test
    fun `the board on a board that got solved passes focus as a winning arrow does`() {
        assertEquals(Focus.GiveUpRow, fallbackFocus(Focus.Board.key, won.copy(giveUp = GiveUpButton.MORDIES_SOLUTION)))
        assertEquals(Focus.NewButton, fallbackFocus(Focus.Board.key, won.copy(giveUp = GiveUpButton.HIDDEN)))
    }

    @Test
    fun `the Give Up row gone from a new game passes focus to the board on a computer`() {
        val keyboardPlay = playing.copy(selection = TileSelection.INITIAL, giveUp = GiveUpButton.DIMMED)
        assertEquals(Focus.Board, fallbackFocus(Focus.GiveUpRow.key, keyboardPlay))
    }

    @Test
    fun `Next at the last step passes focus to Prev`() {
        assertEquals(Focus.ReplayPrev, fallbackFocus(Focus.ReplayNext.key, replay(atStart = false, atEnd = true)))
    }

    @Test
    fun `Prev at the first step and Restart pass focus to Next`() {
        val atStart = replay(atStart = true, atEnd = false)
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.ReplayPrev.key, atStart))
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.ReplayRestart.key, atStart))
    }

    @Test
    fun `the Give Up row passes focus to Next once the solution screen opens`() {
        assertEquals(Focus.ReplayNext, fallbackFocus(Focus.GiveUpRow.key, replay(atStart = true, atEnd = false)))
    }

    @Test
    fun `the Tutorial button gone passes focus as the Give Up row does`() {
        val key = Focus.TutorialReplay.key
        assertEquals(Focus.ReplayNext, fallbackFocus(key, replay(atStart = true, atEnd = false)))
        assertEquals(Focus.Board, fallbackFocus(key, playing.copy(selection = TileSelection.INITIAL)))
        assertEquals(Focus.PageStart, fallbackFocus(key, playing))
    }

    @Test
    fun `anything else goes to the start of the page`() {
        assertEquals(Focus.PageStart, fallbackFocus("nothing-like-this", playing))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.GiveUpRow.key, playing))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.SaveButton.key, playing))
    }

    // GH-289: a redraw under an open dialog
    private val dialogs = listOf(
        WebDialog.Size(listOf(3, 4), current = 4),
        WebDialog.Save("foo", warning = null),
        WebDialog.Load(restore = false, saves = emptyList()),
        WebDialog.DeleteSave("foo"),
        WebDialog.Unsolvable("no"),
        WebDialog.GiveUp,
        WebDialog.Controls(viewmodel.KeyboardLayout.entries.first(), keyList = emptyList()),
        WebDialog.LinkRejected("no"),
        WebDialog.OpenShared(side = 3),
        WebDialog.Update,
        WebDialog.ReplaySave,
    )

    @Test
    fun `a gone or disabled control under an open dialog passes focus to the dialog's default, not the page start`() {
        for (dialog in dialogs) {
            val open = playing.copy(dialog = dialog)
            assertEquals(defaultFocus(dialog), fallbackFocus("nothing-like-this", open), dialog.toString())
            assertEquals(defaultFocus(dialog), fallbackFocus(Focus.TutorialSkip.key, open), dialog.toString())
        }
    }

    @Test
    fun `the rules for a game with no dialog are unchanged`() {
        assertEquals(Focus.PageStart, fallbackFocus(Focus.DialogOk.key, playing))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.DialogCancel.key, playing))
    }

    // GH-288
    private val panel = Guidance(step = 5, text = "Can you beat my results?", controls = true, highlight = null)

    @Test
    fun `on step 5 a winning arrow or the board passes focus to Labels, not Skip`() {
        val step5 = won.copy(guidance = panel, giveUp = GiveUpButton.HIDDEN)
        assertEquals(Focus.TutorialLabels, fallbackFocus(Focus.ArrowButton(Arrow.DOWN, 3).key, step5))
        assertEquals(Focus.TutorialLabels, fallbackFocus(Focus.Board.key, step5))
    }

    @Test
    fun `the panel gone from a won board passes focus as a winning arrow does`() {
        assertEquals(Focus.GiveUpRow, fallbackFocus(Focus.TutorialSkip.key, won.copy(giveUp = GiveUpButton.MORDIES_SOLUTION)))
        assertEquals(Focus.NewButton, fallbackFocus(Focus.TutorialLabels.key, won.copy(giveUp = GiveUpButton.HIDDEN)))
    }

    @Test
    fun `the panel gone from a game in play passes focus to the board on a computer`() {
        val keyboardPlay = playing.copy(selection = TileSelection.INITIAL)
        assertEquals(Focus.Board, fallbackFocus(Focus.TutorialSkip.key, keyboardPlay))
        assertEquals(Focus.PageStart, fallbackFocus(Focus.TutorialSkip.key, playing))
    }
}
