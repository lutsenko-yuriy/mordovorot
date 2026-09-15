package viewmodel

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import testing.FakeBoardModel
import testing.FakeSaveRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pins the property the whole request-channel design rests on (GH-42 WU2, see the plan comment's
 * "Why ordering is preserved"): [viewmodel.ViewModel.uiRequests] is a rendezvous channel, and
 * `ask()` doesn't return until the View has actually answered - so a message the viewModel emits
 * before a prompt is always observed by the UI before the prompt request arrives, not just
 * "eventually, in some order". Asserted as one interleaved list rather than two separate ones
 * (a message list and a prompt-call list can each be individually correct while their relative
 * order has drifted).
 */
class ViewModelOrderingTest {

    @Test
    fun `a message emitted before a prompt is observed before the prompt request arrives`(): Unit = runBlocking {
        val viewModel = ViewModelImpl(FakeBoardModel(), FakeSaveRepository())
        val events = mutableListOf<String>()
        var promptSaveNameCalls = 0

        coroutineScope {
            val handler = launch {
                for (request in viewModel.uiRequests) {
                    when (request) {
                        is UiRequest.ShowMessage -> {
                            events.add("message: ${request.text}")
                            request.respond(Unit)
                        }
                        is UiRequest.PromptSaveName -> {
                            events.add("promptSaveName")
                            promptSaveNameCalls++
                            request.respond(if (promptSaveNameCalls == 1) "my game" else "foo")
                        }
                        is UiRequest.ConfirmSaveBeforeExit -> request.respond(true)
                        else -> {}
                    }
                }
            }
            try {
                assertFailsWith<ExitRequestedException> { viewModel.exitGame() }
            } finally {
                handler.cancel()
            }
        }

        // exitGame -> confirmSaveBeforeExit(true) -> promptForValidSaveName's first attempt
        // ("my game", invalid - the re-prompt message) -> its second attempt ("foo", valid) ->
        // saveGame's own success message -> ExitRequestedException.
        assertEquals(
            listOf(
                "promptSaveName",
                "message: 'my game' isn't a usable save name (no spaces, path separators, or '..') - " +
                    "try again, or press Enter to skip saving.",
                "promptSaveName",
                "message: Saved as 'foo'.",
            ),
            events,
        )
    }
}
