package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import mordie.Mordie
import testing.FakeMordieSearchFactory
import testing.RecordingAnalyticsService
import testing.testViewModel

/** GH-349: `game_restarted`. `input_method` is added by the web's decorator, and the dialog's own events are the page's. */
class ViewModelRestartAnalyticsTest {

    private val original = intArrayOf(1, 2, 3, 6, 4, 5, 0, 7, 8)
    private val analytics = RecordingAnalyticsService()
    private val viewModel = testViewModel(BoardImpl(3), analytics = analytics, mordie = Mordie(FakeMordieSearchFactory()))

    private fun restarted() = analytics.events.filter { it.name == "game_restarted" }.map { it.properties }

    @Test
    fun `a restart tracks the board size, the moves it discarded and how it was asked`() {
        viewModel.newGameFrom(original, sender = null)
        repeat(3) { viewModel.shiftLeft(1) }

        viewModel.restartGame("shortcut", "dont_save")

        assertEquals(
            listOf(mapOf("board_size" to 3, "move_count" to 3, "was_solved" to false, "save_choice" to "dont_save", "opened_from" to "shortcut")),
            restarted(),
        )
    }

    @Test
    fun `from a won board it says so`() {
        viewModel.newGameFrom(original, sender = null)
        viewModel.shiftRight(0)
        viewModel.shiftDown(0)

        viewModel.restartGame("button", "not_asked")

        assertEquals(listOf(true to "not_asked"), restarted().map { it["was_solved"] to it["save_choice"] })
        assertEquals(2, restarted().single()["move_count"])
    }

    @Test
    fun `a dimmed restart tracks nothing`() {
        viewModel.newGameFrom(original, sender = null)

        viewModel.restartGame("button", "not_asked")

        assertEquals(emptyList(), restarted())
    }

    @Test
    fun `restarting again counts the moves made since`() {
        viewModel.newGameFrom(original, sender = null)
        viewModel.shiftLeft(1)
        viewModel.restartGame("button", "saved")
        repeat(2) { viewModel.shiftLeft(1) }

        viewModel.restartGame("button", "saved")

        assertEquals(listOf(1, 2), restarted().map { it["move_count"] })
    }
}
