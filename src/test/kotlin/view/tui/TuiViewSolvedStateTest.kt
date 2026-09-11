package view.tui

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's solved-state view: disabled shift arrows, the "Congratulations
 * ✓" title, and the transition back to the normal board when a load restores an unsolved
 * game. Filled in by `implement` during WU5, once `TuiView` reacts to `Presenter.isSolved()`.
 */
class TuiViewSolvedStateTest {

    @Test
    fun `when the board becomes solved, arrow clicks make no presenter call`() {
        // TODO: Script FakePresenter.isSolved() to return true after a shift
        // TODO: Perform a click on a shift arrow after the board is solved
        // TODO: Verify no further shift* call was recorded
    }

    @Test
    fun `the title changes to Congratulations tick on solve`() {
        // TODO: Script FakePresenter.isSolved() to return true
        // TODO: Trigger a repaint
        // TODO: Verify the rendered frame shows "Congratulations ✓" instead of "Mordovorot"
    }

    @Test
    fun `Save, Load, and Exit remain active after solve`() {
        // TODO: With isSolved() = true, click Save -> verify the Save dialog opens
        // TODO: Click Load -> verify the Load dialog opens
        // TODO: Click Exit -> verify the Exit dialog opens
    }

    @Test
    fun `screen_congratulations fires exactly once on the transition into solved`() {
        // TODO: Script isSolved() false, then true after one shift, then true again on a later repaint
        // TODO: Verify RecordingAnalyticsService recorded screen_congratulations exactly once
    }

    @Test
    fun `loading an unsolved board from the Congratulations screen re-enables the arrows and title`() {
        // TODO: With the view in the solved state, open Load and restore a save
        // TODO: Script FakePresenter.isSolved() to now return false
        // TODO: Verify the next repaint shows "Mordovorot" and arrow clicks call the presenter again
    }
}
