package viewmodel

import board_model.BoardImpl
import board_model.Solvability
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import testing.testViewModel

/** GH-74: [ViewModelImpl.loadGame] and [ViewModelImpl.restoreOnStartup] on an unsolvable save -
 *  the [UiRequest.ResolveUnsolvableSave] choice and each of its three branches. */
class ViewModelUnsolvableLoadTest {

    /** 3x3, tiles 0/2 swapped - odd, unsolvable. Repair swaps positions 0/1. */
    private val oddState = intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8)
    private val repairedState = listOf(1, 2, 0, 3, 4, 5, 6, 7, 8)
    private val unsolvableMessage = "'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board."

    private fun saves() = FakeSaveRepository(mutableMapOf("odd" to SavedBoard(3, oddState.copyOf(), 5)))

    @Test
    fun `loading an unsolvable save explains why and asks what to do`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves(), analytics)
        val ui = FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
            chooseBoardSizeResponses = mutableListOf(null),
        )

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(listOf("odd (3x3) unsolvable" to false), ui.resolveUnsolvableSaveCalls)
        assertEquals(unsolvableMessage, ui.shownMessages.first())
        assertEquals(Event("load_command_used", mapOf("trigger" to "command", "result" to "unsolvable")), analytics.events.first())
    }

    @Test
    fun `repair overwrites the save, keeps the move count and loads the fixed board`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val saves = saves()
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(repairedState, saves.load("odd")!!.state.toList())
        assertEquals(listOf(5), saves.savedMoveCounts)
        assertEquals(repairedState, board.boardArray.toList())
        assertEquals(5, board.moveCount)
        assertTrue(Solvability.isSolvable(board.boardArray))
        assertEquals("Repaired 'odd' (two tiles swapped) and loaded it.", ui.shownMessages.last())
        assertTrue(ui.loadedSaves.all { it == null })
        assertEquals(
            listOf(
                Event("load_command_used", mapOf("trigger" to "command", "result" to "unsolvable")),
                Event(
                    "unsolvable_save_resolved",
                    mapOf("choice" to "repair", "trigger" to "command", "board_size" to 3, "repair_saved" to true),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `a repair whose overwrite fails still loads the fixed board, with a warning`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val saves = saves().apply { saveException = IOException("disk full") }
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves, analytics)
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(repairedState, board.boardArray.toList())
        assertEquals(oddState.toList(), saves.load("odd")!!.state.toList())
        assertEquals("Repaired and loaded 'odd', but could not update the save file: disk full", ui.shownMessages.last())
        assertEquals(
            Event(
                "unsolvable_save_resolved",
                mapOf("choice" to "repair", "trigger" to "command", "board_size" to 3, "repair_saved" to false),
            ),
            analytics.events.last(),
        )
    }

    /** Audit on PR #77: a hand-edited out-of-range size (7x7) can't be restored - the file must
     *  stay as it was rather than be rewritten before the load fails. */
    @Test
    fun `repair leaves the save untouched when the board can't take it`(): Unit = runBlocking {
        val sevenBySeven = IntArray(49) { it }.also { it[0] = 1; it[1] = 0 }
        val saves = FakeSaveRepository(mutableMapOf("big" to SavedBoard(7, sevenBySeven.copyOf(), 2)))
        val viewModel = testViewModel(BoardImpl(4), saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi(resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR))

        ui.drive(viewModel) { viewModel.loadGame("big") }

        assertEquals(emptyList(), saves.saveCalls)
        assertEquals(sevenBySeven.toList(), saves.load("big")!!.state.toList())
    }

    @Test
    fun `new game mid-game asks for the size and starts a fresh game`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves(), analytics)
        val ui = FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
            chooseBoardSizeResponses = mutableListOf(5),
        )

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(listOf(4), ui.chooseBoardSizeCalls)
        assertEquals(listOf("unsolvable_save"), ui.chooseBoardSizeOrigins)
        assertEquals(5, board.squareSide)
        assertEquals(0, board.moveCount)
        assertEquals(
            listOf(
                Event("load_command_used", mapOf("trigger" to "command", "result" to "unsolvable")),
                Event("unsolvable_save_resolved", mapOf("choice" to "new_game", "trigger" to "command", "board_size" to 3)),
                Event("new_game_size_selected", mapOf("size" to 5, "trigger" to "unsolvable_save")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `new game mid-game with no size answer starts fresh at the current size`(): Unit = runBlocking {
        val board = BoardImpl(4).apply { shiftLeft(0) }
        val viewModel = testViewModel(board, saves(), RecordingAnalyticsService())
        val ui = FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
            chooseBoardSizeResponses = mutableListOf(null),
        )

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(4, board.squareSide)
        assertEquals(0, board.moveCount)
    }

    @Test
    fun `exit mid-game runs the normal exit flow`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(4), saves(), analytics)
        val ui = FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT),
            confirmSaveBeforeExitResponses = mutableListOf(false),
        )

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.loadGame("odd") } }

        assertEquals(1, ui.confirmSaveBeforeExitCallCount)
        assertEquals(
            listOf(
                Event("load_command_used", mapOf("trigger" to "command", "result" to "unsolvable")),
                Event("unsolvable_save_resolved", mapOf("choice" to "exit", "trigger" to "command", "board_size" to 3)),
                Event("exit_command_used", mapOf("save_choice" to "declined")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit mid-game whose save fails keeps the current board`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val before = board.boardArray.toList()
        val saves = saves().apply { saveException = IOException("read-only") }
        val viewModel = testViewModel(board, saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi(
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT),
            confirmSaveBeforeExitResponses = mutableListOf(true),
            promptSaveNameResponses = mutableListOf("mine"),
        )

        ui.drive(viewModel) { viewModel.loadGame("odd") }

        assertEquals(4, board.squareSide)
        assertEquals(before, board.boardArray.toList())
    }

    @Test
    fun `repair at startup counts as restored`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves(), analytics)
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(true),
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.REPAIR),
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf("odd (3x3) unsolvable" to true), ui.resolveUnsolvableSaveCalls)
        assertEquals(repairedState, board.boardArray.toList())
        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "unsolvable")),
                Event(
                    "unsolvable_save_resolved",
                    mapOf("choice" to "repair", "trigger" to "startup_prompt", "board_size" to 3, "repair_saved" to true),
                ),
                Event("startup_restore_decision", mapOf("decision" to "restored", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `new game at startup falls through to the startup size prompt`(): Unit = runBlocking {
        val board = BoardImpl(4)
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(board, saves(), analytics)
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(true),
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.NEW_GAME),
            chooseBoardSizeResponses = mutableListOf(5),
        )

        ui.drive(viewModel) { viewModel.restoreOnStartup() }

        assertEquals(listOf("startup"), ui.chooseBoardSizeOrigins)
        assertEquals(5, board.squareSide)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "unsolvable")),
                Event("unsolvable_save_resolved", mapOf("choice" to "new_game", "trigger" to "startup_prompt", "board_size" to 3)),
                Event("startup_restore_decision", mapOf("decision" to "new_game", "save_file_count" to 1)),
                Event("new_game_size_selected", mapOf("size" to 5, "trigger" to "startup_prompt")),
            ),
            analytics.events,
        )
    }

    @Test
    fun `exit at startup quits without asking to save`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val viewModel = testViewModel(BoardImpl(4), saves(), analytics)
        val ui = FakeViewModelUi(
            confirmRestoreResponses = mutableListOf(true),
            resolveUnsolvableSaveResponses = mutableListOf(UnsolvableSaveChoice.EXIT),
        )

        assertFailsWith<ExitRequestedException> { ui.drive(viewModel) { viewModel.restoreOnStartup() } }

        assertEquals(0, ui.confirmSaveBeforeExitCallCount)
        assertEquals(emptyList(), ui.chooseBoardSizeCalls)
        assertEquals(
            listOf(
                Event("startup_restore_prompt_shown", mapOf("save_file_count" to 1)),
                Event("load_command_used", mapOf("trigger" to "startup_prompt", "result" to "unsolvable")),
                Event("unsolvable_save_resolved", mapOf("choice" to "exit", "trigger" to "startup_prompt", "board_size" to 3)),
                Event("startup_restore_decision", mapOf("decision" to "quit", "save_file_count" to 1)),
            ),
            analytics.events,
        )
    }

    @Test
    fun `a solvable save loads without asking`(): Unit = runBlocking {
        val saves = FakeSaveRepository(mutableMapOf("ok" to SavedBoard(3, intArrayOf(1, 2, 0, 3, 4, 5, 6, 7, 8))))
        val viewModel = testViewModel(BoardImpl(4), saves, RecordingAnalyticsService())
        val ui = FakeViewModelUi()

        ui.drive(viewModel) { viewModel.loadGame("ok") }

        assertEquals(emptyList(), ui.resolveUnsolvableSaveCalls)
        assertEquals(listOf("Loaded 'ok'."), ui.shownMessages)
        assertEquals(listOf<String?>("ok"), ui.loadedSaves)
    }
}
