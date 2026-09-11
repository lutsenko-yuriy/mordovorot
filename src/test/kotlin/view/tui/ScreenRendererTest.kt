package view.tui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Scenario stubs for GH-3's frame rendering: turning a [ScreenState] into the box-drawing
 * frame string drawn to the terminal. Pure ScreenState -> String rendering, no terminal
 * involved - assertions run against the returned string. Board-screen cases (WU3) are filled
 * in below; the dialog-overlay cases stay stubs until WU4 adds `Dialog`.
 */
class ScreenRendererTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val renderer = ScreenRenderer()

    @Test
    fun `tile values render 1-based`() {
        // The internal 0-based permutation 0..15 must show up on-screen as 1..16.
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)

        val frame = renderer.render(state, terminalSize)

        for (value in 1..16) assertTrue(frame.contains(value.toString()), "expected tile value $value in frame")
    }

    @Test
    fun `default title renders Mordovorot`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)

        val frame = renderer.render(state, terminalSize)

        assertTrue(frame.contains("Mordovorot"))
    }

    @Test
    fun `solved state renders Congratulations tick with dimmed, non-interactive arrows`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = true)

        val frame = renderer.render(state, terminalSize)

        assertTrue(frame.contains("Congratulations ✓"))
        assertFalse(frame.contains("Mordovorot"))
        // Dimmed arrows are wrapped in the SGR "faint" escape - absent from the enabled frame.
        assertTrue(frame.contains("[2m"))
        val enabledFrame = renderer.render(state.copy(arrowsEnabled = true), terminalSize)
        assertFalse(enabledFrame.contains("[2m"))
    }

    @Test
    fun `the toolbar is always present, solved or not`() {
        val unsolved = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        val solved = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = true)

        for (frame in listOf(renderer.render(unsolved, terminalSize), renderer.render(solved, terminalSize))) {
            assertTrue(frame.contains("Save"))
            assertTrue(frame.contains("Load"))
            assertTrue(frame.contains("Exit"))
        }
    }

    @Test
    fun `the Save dialog overlay includes the overwrite warning only when the name conflicts`() {
        // TODO (WU4): Render a ScreenState with a Save Dialog whose typed name matches an existing save
        // TODO (WU4): Verify the frame string contains an overwrite-warning line
        // TODO (WU4): Render the same dialog with a non-conflicting name
        // TODO (WU4): Verify no warning line is present
    }

    @Test
    fun `the Load dialog overlay shows No saves found when the save list is empty`() {
        // TODO (WU4): Render a ScreenState with a Load Dialog and an empty save list
        // TODO (WU4): Verify the frame string contains "No saves found."
    }

    @Test
    fun `the Exit dialog overlay shows all three buttons`() {
        // TODO (WU4): Render a ScreenState with an Exit Dialog
        // TODO (WU4): Verify the frame string contains Yes, No, and Cancel buttons
    }
}
