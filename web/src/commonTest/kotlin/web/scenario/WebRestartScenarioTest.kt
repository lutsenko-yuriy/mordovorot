package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import storage.SavedMordie
import viewmodel.RestartButton
import viewmodel.TileLabels
import web.Arrow
import web.Focus
import web.ReplaySaveChoice
import web.WebDialog

/** GH-349: the Restart button and its save question. */
@OptIn(ExperimentalCoroutinesApi::class)
class WebRestartScenarioTest {

    private fun TestScope.fresh() = WebHarness(backgroundScope).startOn()

    /** A game with three moves made. */
    private fun TestScope.mid() = fresh().also { it.threeMoves() }

    /** [mid], with the save question open. */
    private fun TestScope.asked() = mid().also { it.game.restart.onRestart("button") }

    /** Loads the save "foo": [ONE_SHIFT_FROM_SOLVED] after a move, 7 moves in, with Mordie's data when [known]. */
    private fun TestScope.loaded(known: Boolean): WebHarness {
        val page = fresh().seed("foo", RESTART_MOVED, moves = 7, mordie = SavedMordie(ONE_SHIFT_FROM_SOLVED, 2, found = false).takeIf { known })
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        return page
    }

    // The button
    @Test
    fun restartIsDimmedAtTheStart() = webTest {
        assertEquals(RestartButton.DIMMED, fresh().state.restart)
    }

    @Test
    fun restartIsLiveAfterAShift() = webTest {
        val page = fresh()
        page.game.onArrow(Arrow.RIGHT, 1)

        assertEquals(RestartButton.LIVE, page.state.restart)
    }

    @Test
    fun aLoadedGameWithItsStartKnownRestartsLive() = webTest {
        assertEquals(RestartButton.LIVE, loaded(known = true).state.restart)
    }

    @Test
    fun aLoadedGameWithoutItsStartIsDimmedUntilTheFirstShift() = webTest {
        val page = loaded(known = false)
        assertEquals(RestartButton.DIMMED, page.state.restart)

        page.game.onArrow(Arrow.RIGHT, 2)

        assertEquals(RestartButton.LIVE, page.state.restart)
    }

    @Test
    fun restartIsHiddenOnTheSolutionScreen() = webTest {
        val page = onTwoMoves()
        page.searchFinds()
        page.fiveShifts()
        page.game.solution.onGiveUp()
        page.game.solution.onGiveUpAnswered(true)

        assertTrue(page.state.solution != null)
        assertEquals(RestartButton.HIDDEN, page.state.restart)
    }

    @Test
    fun restartIsHiddenDuringTheTutorial() = webTest {
        val page = tutorialPage(backgroundScope)

        assertTrue(page.state.guidance != null)
        assertEquals(RestartButton.HIDDEN, page.state.restart)
    }

    @Test
    fun restartIsDisabledWhileADialogIsOpen() = webTest {
        val page = mid()
        page.game.onSave()
        val before = page.state.unfocused()

        // The button is disabled in the page; the game ignores it as well.
        page.game.restart.onRestart("button")

        assertEquals(before, page.state.unfocused())
        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun withMovesRestartAsksToSaveFirst() = webTest {
        val page = mid()
        val before = page.state.unfocused()

        page.game.restart.onRestart("button")

        assertEquals(WebDialog.RestartSave, page.state.dialog)
        assertEquals(Focus.DialogCancel, page.state.focus)
        assertEquals(before, page.state.copy(dialog = null).unfocused())
    }

    @Test
    fun cancelChangesNothing() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.game.restart.onRestart("button")

        page.game.restart.onAnswered(ReplaySaveChoice.CANCEL)

        assertEquals(before, page.state.unfocused())
        assertEquals(3, page.state.moves)
        assertEquals(Focus.RestartButton, page.state.focus)
    }

    @Test
    fun escapeIsCancel() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.game.restart.onRestart("button")

        page.game.onEscape()

        assertNull(page.state.dialog)
        assertEquals(before, page.state.unfocused())
        assertEquals(Focus.RestartButton, page.state.focus)
    }

    @Test
    fun dontSaveRestartsTheGame() = webTest {
        val page = mid()
        val before = page.state

        page.restartDiscarding()

        assertNull(page.state.dialog)
        assertEquals(RESTART_START, page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals(before.side, page.state.side)
        assertEquals(before.labels, page.state.labels)
        assertEquals(RestartButton.DIMMED, page.state.restart)
    }

    @Test
    fun dontSaveKeepsTheSaveName() = webTest {
        val page = fresh().seed("foo", ONE_SHIFT_FROM_SOLVED)
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        page.game.onArrow(Arrow.RIGHT, 1)
        page.restartDiscarding()

        page.game.onSave()

        assertEquals("foo", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun dontSaveClearsUnsavedProgress() = webTest {
        val page = mid()
        assertTrue(page.game.hasUnsavedProgress())

        page.restartDiscarding()

        assertFalse(page.game.hasUnsavedProgress())
    }

    @Test
    fun saveOpensTheSaveDialogWithTheCurrentName() = webTest {
        val page = fresh().seed("foo", ONE_SHIFT_FROM_SOLVED)
        page.game.onLoad()
        page.game.onLoadChosen("foo")
        page.game.onArrow(Arrow.RIGHT, 1)

        page.restartWith(ReplaySaveChoice.SAVE)

        assertEquals("foo", (page.state.dialog as WebDialog.Save).name)
    }

    @Test
    fun aSuccessfulSaveRestartsTheGame() = webTest {
        val page = mid()
        val played = page.state.tiles
        page.restartWith(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("mine")

        val saved = page.saves.load("mine")!!
        assertEquals(shown(saved.state), played)
        assertEquals(3, saved.moveCount)
        assertEquals(RESTART_START, page.state.tiles)
        assertEquals(0, page.state.moves)
        assertNull(page.state.dialog)
    }

    @Test
    fun aCancelledSaveRestartsNothing() = webTest {
        val page = mid()
        val before = page.state.unfocused()
        page.restartWith(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered(null)

        assertEquals(before, page.state.unfocused())
        assertEquals(Focus.RestartButton, page.state.focus)
    }

    @Test
    fun aNameWithSpacesRestartsNothing() = webTest {
        val page = mid()
        val played = page.state.tiles
        page.restartWith(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("two words")

        assertEquals(played, page.state.tiles)
        assertEquals(3, page.state.moves)
        assertNull(page.state.dialog)
        assertTrue(page.saves.listSaves().isEmpty())
    }

    @Test
    fun aFailedSaveRestartsNothing() = webTest {
        val page = mid()
        val played = page.state.tiles
        page.store.setException = IllegalStateException("quota")
        page.restartWith(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("mine")

        assertEquals(played, page.state.tiles)
        assertEquals(3, page.state.moves)
    }

    @Test
    fun aWonBoardRestartsAtOnce() = webTest {
        val page = fresh()
        page.game.onArrow(Arrow.LEFT, 0)
        assertTrue(page.state.solved)

        page.game.restart.onRestart("button")

        assertNull(page.state.dialog)
        assertFalse(page.state.solved)
        assertEquals(RESTART_START, page.state.tiles)
        assertEquals(0, page.state.moves)
    }

    @Test
    fun aLoadedGameGoesBackToMordiesOriginal() = webTest {
        val page = loaded(known = true)
        page.game.onArrow(Arrow.RIGHT, 2)

        page.restartDiscarding()

        assertEquals(RESTART_START, page.state.tiles)
        assertEquals(0, page.state.moves)
    }

    @Test
    fun aLoadedGameWithoutAStartGoesBackToTheBoardAsLoaded() = webTest {
        val page = loaded(known = false)
        assertEquals(7, page.state.moves)
        page.game.onArrow(Arrow.RIGHT, 2)

        page.restartDiscarding()

        assertEquals(shown(RESTART_MOVED), page.state.tiles)
        assertEquals(7, page.state.moves)
    }

    @Test
    fun aSharedBoardRestartsToTheSharedArrangement() = webTest {
        val page = WebHarness(backgroundScope).startWithLink(4, RESTART_LINKED, "pairs")
        page.game.share.onSharedLabelsAnswered(true)
        page.game.onArrow(Arrow.RIGHT, 1)

        page.restartDiscarding()

        assertEquals(shown(RESTART_LINKED), page.state.tiles)
        assertEquals(0, page.state.moves)
        assertEquals(TileLabels.PAIRS, page.state.labels)
    }

    @Test
    fun restartKeepsTheTileLabels() = webTest {
        val page = fresh()
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = true)
        page.game.onArrow(Arrow.RIGHT, 1)

        page.restartDiscarding()

        assertEquals(TileLabels.LETTERS, page.state.labels)
        assertEquals(RESTART_START, page.state.tiles)
    }

    @Test
    fun aSecondRestartReturnsToTheSameStart() = webTest {
        val page = mid()
        page.restartDiscarding()
        page.game.onArrow(Arrow.DOWN, 2)
        page.game.onArrow(Arrow.RIGHT, 3)

        page.restartDiscarding()

        assertEquals(RESTART_START, page.state.tiles)
        assertEquals(0, page.state.moves)
    }

    @Test
    fun aHeldMoveIsDroppedByRestart() = webTest {
        val page = WebHarness(backgroundScope, motion = true).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)
        page.game.labels.open()
        page.game.labels.onSelected(TileLabels.LETTERS)
        page.game.labels.onAnswered(ok = false)
        assertTrue(page.waving)
        page.game.onArrow(Arrow.LEFT, 0)

        page.restartDiscarding()
        advanceTimeBy(2_000)
        runCurrent()

        assertEquals(RESTART_START, page.state.tiles)
        assertFalse(page.state.solved)
        assertEquals(0, page.state.moves)
    }
}
