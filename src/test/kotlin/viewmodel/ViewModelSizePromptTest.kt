package viewmodel

import testing.FakeBoardModel
import testing.FakeSaveRepository
import testing.FakeViewModelUi
import testing.RecordingAnalyticsService
import kotlinx.coroutines.runBlocking
import kotlin.test.Test

/**
 * Covers GH-44's startup size prompt - the interactive size choice [ViewModelImpl.restoreOnStartup]
 * raises when no `--size` flag was given and no save was restored. Stubs only - `implement`
 * fills these in as it builds the size-prompt flow (plan comment on GH-44, WU2).
 */
class ViewModelSizePromptTest {

    @Test
    fun `no saves, no --size given - prompts for a size and starts a new game at the chosen size`(): Unit = runBlocking {
        // TODO: Build ViewModelImpl(FakeBoardModel(), FakeSaveRepository(), RecordingAnalyticsService(), sizePromptEnabled = true) with no saves.
        // TODO: Drive restoreOnStartup() via FakeViewModelUi, answering the size-choice request with 3.
        // TODO: Verify the board started a new game at side 3.
        // TODO: Verify new_game_size_selected {size: 3, trigger: "startup_prompt"} was tracked.
    }

    @Test
    fun `saves exist but restore is declined - the size prompt runs after and starts a new game at the chosen size`(): Unit = runBlocking {
        // TODO: Seed one save; decline the restore confirm.
        // TODO: Answer the subsequent size-choice request with 5.
        // TODO: Verify the board started a new game at side 5.
        // TODO: Verify new_game_size_selected {size: 5, trigger: "startup_prompt"} was tracked.
    }

    @Test
    fun `restore is accepted - the size prompt never runs, the restored save's size applies`(): Unit = runBlocking {
        // TODO: Seed a save; confirm restore.
        // TODO: Verify no size-choice request was ever asked.
        // TODO: Verify no new_game_size_selected event fired.
    }

    @Test
    fun `--size given at launch - the size prompt never runs`(): Unit = runBlocking {
        // TODO: Build ViewModelImpl with sizePromptEnabled = false, no saves.
        // TODO: Drive restoreOnStartup().
        // TODO: Verify no size-choice request was asked and no new_game_size_selected fired.
    }

    @Test
    fun `size prompt - invalid input re-prompts instead of falling back to a default`(): Unit = runBlocking {
        // TODO: No saves, sizePromptEnabled = true.
        // TODO: Answer the size-choice request first with an out-of-range/non-numeric value, then a valid one.
        // TODO: Verify the prompt was asked twice and the board ends up at the valid size.
    }

    @Test
    fun `size prompt - blank input-EOF keeps the current size without starting a new game`(): Unit = runBlocking {
        // TODO: No saves, sizePromptEnabled = true.
        // TODO: Answer the size-choice request with null (blank/EOF sentinel).
        // TODO: Verify no new game was started and no new_game_size_selected fired.
    }
}
