package presenter

import storage.SavedBoard
import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Hard problem 3 (GH-42 WU2): `saveGame`/`loadGame`/`restoreOnStartup` all suspend inside
 * [PresenterImpl.ask], whose `catch (e: Exception)` catch-all would otherwise swallow a
 * [kotlinx.coroutines.CancellationException] - `CancellationException` *is* a `java.lang.Exception`
 * on the JVM (via `IllegalStateException`). Each method's `catch (e: CancellationException) {
 * throw e }` guard, immediately before its catch-all, is what these tests pin: cancelling the
 * scope while a call is suspended in `ask` must not be recorded as a failed save/load, or emit
 * the catch-all's error message (which would itself hang, being another suspend `ask` call with
 * nothing left to drain it once the coroutine is cancelled).
 *
 * No handler drains [Presenter.uiRequests] in any of these tests - that's deliberate; each
 * call is cancelled while suspended waiting for an answer nothing will ever give it.
 *
 * Each scenario below reaches its suspending `showMessage` call *after* already tracking a
 * legitimate analytics event synchronously (that part of the work genuinely happened, and
 * cancellation doesn't undo it) - so the guard's effect isn't "no event at all", it's "no
 * *second*, spurious `result: error` event" from the catch-all misreading the resumed
 * [kotlinx.coroutines.CancellationException] as an ordinary failure.
 */
class PresenterCancellationTest {

    @Test
    fun `saveGame cancelled while suspended in ask does not also track a spurious result=error`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val presenter = PresenterImpl(FakeBoardModel(), FakeSaveRepository(), analytics)

        val job = launch { presenter.saveGame("foo") }
        yield() // let saveGame reach ask()'s suspension point before cancelling
        job.cancelAndJoin()

        assertEquals(
            listOf(Event("save_command_used", mapOf("result" to "success", "overwrote_existing" to false))),
            analytics.events,
        )
    }

    @Test
    fun `loadGame cancelled while suspended in ask does not also track a spurious result=error`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val saves = FakeSaveRepository(mutableMapOf("missing-on-purpose" to SavedBoard(4, IntArray(16) { it })))
        val presenter = PresenterImpl(FakeBoardModel(), saves, analytics)

        val job = launch { presenter.loadGame("not-found") }
        yield()
        job.cancelAndJoin()

        assertEquals(
            listOf(Event("load_command_used", mapOf("trigger" to "command", "result" to "not_found"))),
            analytics.events,
        )
    }

    @Test
    fun `offerStartupRestore cancelled while suspended in ask does not track a decision`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val saves = FakeSaveRepository(mutableMapOf("foo" to SavedBoard(4, IntArray(16) { it })))
        val presenter = PresenterImpl(FakeBoardModel(), saves, analytics)

        val job = launch { presenter.restoreOnStartup() }
        yield()
        job.cancelAndJoin()

        // Only startup_restore_prompt_shown may have fired (it's tracked before the suspending
        // confirmRestore ask()) - startup_restore_decision must not, since that would misreport
        // a cancelled launch as a completed new_game/restored decision.
        assertEquals(emptyList(), analytics.events.filter { it.name == "startup_restore_decision" })
    }
}
