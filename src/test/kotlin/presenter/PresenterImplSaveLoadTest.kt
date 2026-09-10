package presenter

import kotlin.test.Test

/**
 * Scenario stubs for GH-6 (save/load console commands). Filled in by `implement`
 * during WU3, once `Presenter.saveGame`/`loadGame`, `storage.SaveRepository`, and
 * `analytics.AnalyticsService` wiring exist. See the plan comment on GH-6 for the
 * full test-double list (`FakeSaveRepository`, `RecordingAnalyticsService`).
 */
class PresenterImplSaveLoadTest {

    @Test
    fun `saveGame creates a new file when none exists`() {
        // TODO: Given: FakeSaveRepository with no existing save named "foo"
        // TODO: 1. Call presenter.saveGame("foo")
        // TODO: 2. Verify FakeSaveRepository.save("foo", board.boardArray, ...) was called
        // TODO: 3. Verify RecordingAnalyticsService captured save_command_used{overwrote_existing=false}
    }

    @Test
    fun `saveGame overwrites an existing file`() {
        // TODO: Given: FakeSaveRepository seeded with an existing save named "foo"
        // TODO: 1. Call presenter.saveGame("foo")
        // TODO: 2. Verify the save was overwritten with the current board state
        // TODO: 3. Verify save_command_used{overwrote_existing=true}
    }

    @Test
    fun `loadGame restores the board on a known save name`() {
        // TODO: Given: FakeSaveRepository seeded with a saved arrangement different from the board's current state
        // TODO: 1. Call presenter.loadGame("foo")
        // TODO: 2. Verify the board's state now matches the saved arrangement
        // TODO: 3. Verify load_command_used{trigger=command, result=success}
    }

    @Test
    fun `loadGame leaves the board unchanged on an unknown save name`() {
        // TODO: Given: FakeSaveRepository with no save named "missing"; capture the board's current state
        // TODO: 1. Call presenter.loadGame("missing")
        // TODO: 2. Verify the board's state is unchanged
        // TODO: 3. Verify view.showMessage(...) was called listing available save names
        // TODO: 4. Verify load_command_used{trigger=command, result=not_found}
    }
}
