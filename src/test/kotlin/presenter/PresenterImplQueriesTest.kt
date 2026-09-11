package presenter

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's additive, read-only `Presenter` query methods
 * (`listSaves`, `saveExists`, `isSolved`) that the mouse-driven TUI needs and the
 * console UI never had to ask for. Filled in by `implement` during WU1, once these
 * members exist on `Presenter`/`PresenterImpl`.
 */
class PresenterImplQueriesTest {

    @Test
    fun `listSaves delegates to the save repository`() {
        // TODO: Seed FakeSaveRepository with a few save names
        // TODO: Call presenter.listSaves()
        // TODO: Verify it returns exactly those names
    }

    @Test
    fun `listSaves degrades to an empty list on a repository failure`() {
        // TODO: Script FakeSaveRepository.listSaves() to throw
        // TODO: Call presenter.listSaves()
        // TODO: Verify it returns an empty list rather than throwing
    }

    @Test
    fun `saveExists delegates to the save repository`() {
        // TODO: Seed FakeSaveRepository so "foo" exists and "bar" does not
        // TODO: Verify presenter.saveExists("foo") is true and presenter.saveExists("bar") is false
    }

    @Test
    fun `saveExists degrades to false on a repository failure`() {
        // TODO: Script FakeSaveRepository.exists() to throw
        // TODO: Call presenter.saveExists(name)
        // TODO: Verify it returns false rather than throwing
    }

    @Test
    fun `isSolved delegates to board isCorrect`() {
        // TODO: Seed FakeBoardModel so isCorrect() returns true, then false
        // TODO: Verify presenter.isSolved() matches board.isCorrect() in both cases
    }
}
