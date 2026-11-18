package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import web.Arrow
import web.Focus
import web.ReplaySaveChoice
import web.WebDialog

/** GH-349: the R key, the button's key label and focus. */
class WebRestartKeysScenarioTest {

    /** A game with a shift made. */
    private fun TestScope.mid(touch: Boolean = false) = WebHarness(backgroundScope, touch = touch).startOn().also { it.game.onArrow(Arrow.RIGHT, 1) }

    @Test
    fun rRestartsAGameWithMoves() = webTest {
        val page = mid()

        assertTrue(page.press("r", Focus.Board))

        assertEquals(WebDialog.RestartSave, page.state.dialog)
    }

    @Test
    fun rOnAWonBoardRestartsAtOnce() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        assertTrue(page.state.solved)

        page.press("r", Focus.GiveUpRow)

        assertNull(page.state.dialog)
        assertFalse(page.state.solved)
        assertEquals(RESTART_START, page.state.tiles)
    }

    @Test
    fun theRussianKeyAtKeyRRestarts() = webTest {
        val page = mid()

        assertTrue(page.press("к", Focus.Board, code = "KeyR"))

        assertEquals(WebDialog.RestartSave, page.state.dialog)
    }

    @Test
    fun controlROrCommandRIsLeftToTheBrowser() = webTest {
        val page = mid()

        assertFalse(page.press("r", Focus.Board, modified = true))

        assertNull(page.state.dialog)
        assertEquals(1, page.state.moves)
    }

    @Test
    fun rDoesNothingOnTouch() = webTest {
        val page = mid(touch = true)

        assertFalse(page.press("r"))

        assertNull(page.state.dialog)
        assertEquals(1, page.state.moves)
    }

    @Test
    fun rDoesNothingDuringTheTutorial() = webTest {
        val page = tutorialPage(backgroundScope)
        val before = page.state.unfocused()

        page.press("r", Focus.Board)

        assertEquals(before, page.state.unfocused())
        assertTrue(page.props("screen_restart_save_dialog").isEmpty())
    }

    @Test
    fun rDoesNothingWhileADialogIsOpen() = webTest {
        val page = mid()
        page.game.onSave()

        assertFalse(page.press("r"))

        assertTrue(page.state.dialog is WebDialog.Save)
    }

    @Test
    fun rDoesNothingWhileRestartIsDimmed() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.press("r", Focus.Board)

        assertNull(page.state.dialog)
        assertTrue(page.props("screen_restart_save_dialog").isEmpty())
    }

    @Test
    fun onTheSolutionScreenWithTheBoardFocusedRIsTheReplaysRestart() = webTest {
        val page = onTwoMoves()
        page.searchFinds()
        page.fiveShifts()
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)
        page.game.solution.onReplayNext()
        assertFalse(page.state.solution!!.atStart)

        assertTrue(page.press("r", Focus.Board))

        assertTrue(page.state.solution!!.atStart)
        assertNull(page.state.dialog)
    }

    @Test
    fun aHeldRIsOnePress() = webTest {
        val page = mid()
        assertFalse(page.press("r", Focus.Board, repeat = true))
        assertNull(page.state.dialog)

        page.press("r", Focus.Board)
        page.press("r", Focus.DialogCancel, repeat = true)

        assertEquals(WebDialog.RestartSave, page.state.dialog)
        assertEquals(1, page.props("screen_restart_save_dialog").size)
    }

    @Test
    fun theToolbarNamesTheKeyOnAComputer() = webTest {
        assertEquals("R", mid().state.restartKey)
        assertNull(mid(touch = true).state.restartKey)
    }

    @Test
    fun aSuccessfulRestartFocusesTheBoardOnAComputer() = webTest {
        val page = mid()

        page.restartDiscarding()

        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun onTouchFocusGoesToNew() = webTest {
        val page = mid(touch = true)

        page.restartDiscarding()

        assertEquals(Focus.NewButton, page.state.focus)
    }

    @Test
    fun cancelAfterRGoesBackWhereRWasPressed() = webTest {
        val page = mid()
        page.press("r", Focus.SaveButton)

        page.game.restart.onAnswered(ReplaySaveChoice.CANCEL)

        assertEquals(Focus.Restore(Focus.SaveButton.key), page.state.focus)
    }
}
