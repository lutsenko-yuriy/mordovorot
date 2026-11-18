package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.TestScope
import web.Arrow
import web.ReplaySaveChoice

/** GH-349: the analytics of a restart (docs/ANALYTICS_EVENTS.md). */
class WebRestartAnalyticsScenarioTest {

    private fun TestScope.mid() = WebHarness(backgroundScope).startOn().also { it.threeMoves() }

    private fun WebHarness.drag(row: Int, col: Int, x: Double, y: Double) {
        game.onDragStart(row, col, 100.0, 100.0, 40.0)
        game.onDragMove(x, y)
        game.onDragEnd()
    }

    @Test
    fun theSaveQuestionSendsItsScreenView() = webTest {
        val page = mid()

        page.game.restart.onRestart("button")

        assertEquals(listOf(mapOf("opened_from" to "button", "move_count" to 3)), page.ownProps("screen_restart_save_dialog"))
    }

    @Test
    fun pressingRSaysShortcut() = webTest {
        val page = mid()

        page.press("r", web.Focus.Board)

        assertEquals(listOf(mapOf("opened_from" to "shortcut", "move_count" to 3)), page.ownProps("screen_restart_save_dialog"))
    }

    @Test
    fun dontSaveSendsGameRestarted() = webTest {
        val page = mid()

        page.restartDiscarding()

        assertEquals(
            listOf(mapOf("board_size" to 4, "move_count" to 3, "was_solved" to false, "save_choice" to "dont_save", "opened_from" to "button")),
            page.ownProps("game_restarted"),
        )
    }

    @Test
    fun aSavedRestartSendsGameRestarted() = webTest {
        val page = mid()
        page.restartWith(ReplaySaveChoice.SAVE)

        page.game.onSaveAnswered("mine")

        assertEquals(listOf("saved"), page.props("game_restarted").map { it["save_choice"] })
    }

    @Test
    fun aWonBoardSendsNotAsked() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)

        page.game.restart.onRestart("button")

        assertEquals(
            listOf(mapOf("board_size" to 4, "move_count" to 1, "was_solved" to true, "save_choice" to "not_asked", "opened_from" to "button")),
            page.ownProps("game_restarted"),
        )
        assertTrue(page.props("screen_restart_save_dialog").isEmpty())
    }

    @Test
    fun cancelSendsDialogCancelled() = webTest {
        val page = mid()
        page.game.restart.onRestart("button")

        page.game.restart.onAnswered(ReplaySaveChoice.CANCEL)

        assertEquals(listOf(mapOf("dialog" to "restart_save", "via" to "button")), page.ownProps("dialog_cancelled"))
        assertTrue(page.props("game_restarted").isEmpty())
    }

    @Test
    fun escapeSendsDialogCancelled() = webTest {
        val page = mid()
        page.game.restart.onRestart("button")

        page.game.onEscape()

        assertEquals(listOf(mapOf("dialog" to "restart_save", "via" to "escape")), page.ownProps("dialog_cancelled"))
    }

    @Test
    fun theSaveDialogSaysItWasOpenedFromRestart() = webTest {
        val page = mid()

        page.restartWith(ReplaySaveChoice.SAVE)

        assertEquals(listOf("restart"), page.props("screen_save_dialog").map { it["opened_from"] })
    }

    @Test
    fun aDimmedRestartSendsNothing() = webTest {
        val page = WebHarness(backgroundScope).startOn()

        page.press("r", web.Focus.Board)
        page.game.restart.onRestart("button")

        assertTrue(page.props("screen_restart_save_dialog").isEmpty())
        assertTrue(page.props("game_restarted").isEmpty())
    }

    @Test
    fun boardSolvedCarriesTheRestartCount() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.LEFT, 0)
        page.game.restart.onRestart("button")
        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(listOf(0, 1), page.props("board_solved").map { it["restart_count"] })
    }

    @Test
    fun aNewGameStartsTheCountAgain() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.game.onArrow(Arrow.RIGHT, 1)
        page.restartDiscarding()
        page.game.onNew()
        page.game.onSizeChosen(4)
        // The new game's board is random: put the near-solved one in its place, as a game begun from it.
        page.board.restoreState(ONE_SHIFT_FROM_SOLVED, 0)

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(listOf(0), page.props("board_solved").map { it["restart_count"] })
    }

    @Test
    fun aLoadStartsTheCountAgain() = webTest {
        val page = WebHarness(backgroundScope).startOn().seed("near", ONE_SHIFT_FROM_SOLVED)
        page.game.onArrow(Arrow.RIGHT, 1)
        page.restartDiscarding()
        page.game.onLoad()
        page.game.onLoadChosen("near")

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(listOf(0), page.props("board_solved").map { it["restart_count"] })
    }

    @Test
    fun shiftSourcesCountOnlySinceTheRestart() = webTest {
        val page = WebHarness(backgroundScope).startOn()
        page.drag(1, 0, 140.0, 100.0)
        page.restartDiscarding()

        page.game.onArrow(Arrow.LEFT, 0)

        assertEquals(listOf("arrows"), page.props("board_solved").map { it["shift_input"] })
    }
}
