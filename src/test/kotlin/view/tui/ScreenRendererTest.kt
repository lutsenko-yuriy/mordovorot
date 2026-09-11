package view.tui

import kotlin.test.Test

/**
 * Scenario stubs for GH-3's frame rendering: turning a [ScreenState] into the box-drawing
 * frame string drawn to the terminal. Pure ScreenState -> String rendering, no terminal
 * involved - assertions run against the returned string. Filled in by `implement` during
 * WU3 (board) and WU4 (dialogs), once `ScreenState`/`Dialog`/`ScreenRenderer` exist.
 */
class ScreenRendererTest {

    @Test
    fun `tile values render 1-based`() {
        // TODO: Render a ScreenState for a board holding the internal 0-based permutation
        // TODO: Verify the frame string shows values 1..16, not 0..15 (GH-10 dialect)
    }

    @Test
    fun `default title renders Mordovorot`() {
        // TODO: Render a ScreenState for an unsolved board
        // TODO: Verify the frame string contains the title "Mordovorot"
    }

    @Test
    fun `solved state renders Congratulations tick with dimmed, non-interactive arrows`() {
        // TODO: Render a ScreenState with arrowsEnabled = false (solved)
        // TODO: Verify the frame string contains "Congratulations ✓" instead of "Mordovorot"
        // TODO: Verify the arrow glyphs are rendered in their dimmed/disabled form
    }

    @Test
    fun `the toolbar is always present, solved or not`() {
        // TODO: Render both a solved and an unsolved ScreenState
        // TODO: Verify both frame strings contain the Save/Load/Exit toolbar
    }

    @Test
    fun `the Save dialog overlay includes the overwrite warning only when the name conflicts`() {
        // TODO: Render a ScreenState with a Save Dialog whose typed name matches an existing save
        // TODO: Verify the frame string contains an overwrite-warning line
        // TODO: Render the same dialog with a non-conflicting name
        // TODO: Verify no warning line is present
    }

    @Test
    fun `the Load dialog overlay shows No saves found when the save list is empty`() {
        // TODO: Render a ScreenState with a Load Dialog and an empty save list
        // TODO: Verify the frame string contains "No saves found."
    }

    @Test
    fun `the Exit dialog overlay shows all three buttons`() {
        // TODO: Render a ScreenState with an Exit Dialog
        // TODO: Verify the frame string contains Yes, No, and Cancel buttons
    }
}
