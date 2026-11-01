package viewmodel

import board_model.BoardImpl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import storage.SavedBoard
import testing.FakeSaveRepository
import testing.testViewModel

/**
 * Pins the property the whole request-channel design rests on (GH-42 WU2, see the plan comment's
 * "Why ordering is preserved"): [viewmodel.GameViewModel.uiRequests] is a rendezvous channel, and
 * `ask()` doesn't return until the View has actually answered - so a message the viewModel emits
 * before a prompt is always observed by the UI before the prompt request arrives, not just
 * "eventually, in some order". Asserted as one interleaved list rather than two separate ones
 * (a message list and a prompt-call list can each be individually correct while their relative
 * order has drifted). Checked over an unsolvable load.
 */
class ViewModelOrderingTest {

    @Test
    fun `a message emitted before a prompt is observed before the prompt request arrives`() = runTest {
        val saves = FakeSaveRepository(mutableMapOf("odd" to SavedBoard(3, intArrayOf(2, 1, 0, 3, 4, 5, 6, 7, 8), 5)))
        val viewModel = testViewModel(BoardImpl(4), saves)
        val events = mutableListOf<String>()

        coroutineScope {
            val handler = launch {
                for (request in viewModel.uiRequests) {
                    when (request) {
                        is UiRequest.ShowMessage -> {
                            events.add("message: ${request.text}")
                            request.respond(Unit)
                        }
                        is UiRequest.ResolveUnsolvableSave -> {
                            events.add("resolveUnsolvableSave")
                            request.respond(UnsolvableSaveChoice.NEW_GAME)
                        }
                        is UiRequest.ChooseBoardSize -> request.respond(null)
                        else -> {}
                    }
                }
            }
            try {
                viewModel.loadGame("odd")
            } finally {
                handler.cancel()
            }
        }

        assertEquals(
            listOf(
                "message: 'odd (3x3)' can't be solved - no sequence of shifts reaches the solved board.",
                "resolveUnsolvableSave",
            ),
            events.take(2),
        )
    }
}
