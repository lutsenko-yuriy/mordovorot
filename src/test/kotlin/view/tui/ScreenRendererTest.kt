package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
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
        // A dimmed arrow is wrapped in the SGR "faint on/off" pair around the actual glyph -
        // asserting on that exact pairing (not just the dim-on code appearing anywhere) so a
        // broken DIM_ON/DIM_OFF wrapping can't pass this test by accident (round-3 audit finding
        // on PR #22: the previous assertions embedded a raw ESC byte and were vacuous - they'd
        // have stayed green even with dimming completely broken).
        val dimmedUpArrow = "[2m▲[22m"
        assertTrue(frame.contains(dimmedUpArrow))
        val enabledFrame = renderer.render(state.copy(arrowsEnabled = true), terminalSize)
        assertFalse(enabledFrame.contains(dimmedUpArrow))
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
        val base = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        val saveButtons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel"))

        val conflicting = base.copy(
            dialog = Dialog(
                kind = Dialog.Kind.SAVE,
                title = "Save game",
                message = "'foo' already exists - it will be overwritten.",
                textFieldValue = "foo",
                buttons = saveButtons,
            ),
        )
        assertTrue(renderer.render(conflicting, terminalSize).contains("already exists"))

        val fresh = base.copy(dialog = Dialog(Dialog.Kind.SAVE, "Save game", textFieldValue = "bar", buttons = saveButtons))
        assertFalse(renderer.render(fresh, terminalSize).contains("already exists"))
    }

    @Test
    fun `the Load dialog overlay shows No saves found when the save list is empty`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(
            dialog = Dialog(
                kind = Dialog.Kind.LOAD,
                title = "Load game",
                message = "No saves found.",
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            ),
        )

        assertTrue(renderer.render(state, terminalSize).contains("No saves found."))
    }

    @Test
    fun `the Exit dialog overlay shows all three buttons`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(
            dialog = Dialog(
                kind = Dialog.Kind.EXIT,
                title = "Save before quitting?",
                buttons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel")),
            ),
        )

        val frame = renderer.render(state, terminalSize)
        assertTrue(frame.contains("Yes"))
        assertTrue(frame.contains("No"))
        assertTrue(frame.contains("Cancel"))
    }

    @Test
    fun `the keyboard cursor highlights exactly its arrow, in the cursor color, and no other`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
            .copy(cursor = ArrowCursor(Edge.TOP, 2))

        val frame = renderer.render(state, terminalSize)

        val coloredUpArrow = "[93m▲[39m"
        assertTrue(frame.contains(coloredUpArrow))
        // Only one arrow is colored - count occurrences of the color wrapper against the total
        // number of up-arrow glyphs, so a bug that highlights every arrow can't pass this test
        // by accident.
        assertEquals(1, Regex(Regex.escape(coloredUpArrow)).findAll(frame).count())
        val plainFrame = renderer.render(state.copy(cursor = null), terminalSize)
        assertFalse(plainFrame.contains(coloredUpArrow))
    }

    @Test
    fun `toolbarShortcuts renders F-key labels instead of the plain ones, in the shortcut color`() {
        val plain = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        val shortcuts = plain.copy(toolbarShortcuts = true)

        assertTrue(renderer.render(plain, terminalSize).contains("[ Save ]"))
        assertFalse(renderer.render(plain, terminalSize).contains("[Save F5]"))
        // Plain (mouse-mode) toolbar labels never carry the shortcut color.
        assertFalse(renderer.render(plain, terminalSize).contains("[96m"))

        val shortcutFrame = renderer.render(shortcuts, terminalSize)
        assertTrue(shortcutFrame.contains("[Save F5]"))
        assertTrue(shortcutFrame.contains("[Load F6]"))
        assertTrue(shortcutFrame.contains("[Exit ESC]"))
        // Every shortcut-colored span opens with the on-code and closes with the off-code,
        // folded onto the label's first/last character (Canvas.putColored) - one matched pair
        // per button.
        assertEquals(3, Regex(Regex.escape("[96m")).findAll(shortcutFrame).count())
        assertEquals(3, Regex(Regex.escape("[39m")).findAll(shortcutFrame).count())
    }

    @Test
    fun `the controls hint renders two rows below the status message, not on a fixed terminal row`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
            .copy(controlsHint = "Arrows: move")
        val layout = BoardLayout(terminalSize, squareSide = 4)

        val frame = renderer.render(state, terminalSize)

        val hintRow = frame.removePrefix("[2J[H").split("\r\n")[layout.toolbarRow + 3]
        assertTrue(hintRow.startsWith("Arrows: move"))
    }

    @Test
    fun `the controls hint never overwrites the toolbar or the status message`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
            .copy(controlsHint = "Arrows: move", message = "Saved as 'x'.")

        val frame = renderer.render(state, terminalSize)

        assertTrue(frame.contains("Saved as 'x'."))
        assertTrue(frame.contains("[ Save ]"))
        assertTrue(frame.contains("Arrows: move"))
    }

    @Test
    fun `no controls hint is drawn when the state carries none`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)

        assertFalse(renderer.render(state, terminalSize).contains("Arrows: move"))
    }

    @Test
    fun `a focused dialog button renders reverse video, and an unfocused one does not`() {
        val base = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        val exitButtons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel"))
        val state = base.copy(
            dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = exitButtons, focusedButtonId = "no"),
        )

        val frame = renderer.render(state, terminalSize)

        assertTrue(frame.contains("[7m[ No ][27m"))
        assertFalse(frame.contains("[7m[ Yes ][27m"))
        assertTrue(frame.contains("[ Yes ]")) // unfocused button still renders plainly
    }

    @Test
    fun `the Save dialog's text field appends the focus marker only when focused`() {
        val base = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false)
        val saveButtons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel"))

        val focused = base.copy(
            dialog = Dialog(Dialog.Kind.SAVE, "Save game", textFieldValue = "foo", buttons = saveButtons, textFieldFocused = true),
        )
        assertTrue(renderer.render(focused, terminalSize).contains("Name: foo_ ◀"))

        val unfocused = base.copy(
            dialog = Dialog(Dialog.Kind.SAVE, "Save game", textFieldValue = "foo", buttons = saveButtons, textFieldFocused = false),
        )
        assertFalse(renderer.render(unfocused, terminalSize).contains("Name: foo_ ◀"))
        assertTrue(renderer.render(unfocused, terminalSize).contains("Name: foo_"))
    }

    @Test
    fun `a highlighted button clipped by a narrow terminal still closes its reverse video`() {
        // A terminal too narrow to fit the Exit dialog's buttons in full pushes the last one
        // (focused, here) off the right edge of the canvas - regression test for a bug where
        // REVERSE_OFF was only ever written to text's own last index, never reached when that
        // index falls outside the canvas, leaking reverse video into every row/frame after it.
        val narrow = TerminalSize(columns = 18, rows = 18)
        val exitButtons = listOf(DialogButtonSpec("yes", "Yes"), DialogButtonSpec("no", "No"), DialogButtonSpec("cancel", "Cancel"))
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(
            dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = exitButtons, focusedButtonId = "cancel"),
        )

        val frame = renderer.render(state, narrow)

        val onCount = Regex(Regex.escape("[7m")).findAll(frame).count()
        val offCount = Regex(Regex.escape("[27m")).findAll(frame).count()
        assertEquals(onCount, offCount, "every reverse-video ON must have a matching OFF, even when clipped")
    }

    @Test
    fun `a focused text field with a long name keeps its caret, not just the focus marker`() {
        // Regression test: the box used to be sized from "Name: <value>_" alone, so appending
        // the focus marker overflowed maxLineWidth and truncate() swapped the tail - including
        // the caret - for "…" the moment the field gained focus.
        val longName = "a".repeat(32)
        val saveButtons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel"))
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(
            dialog = Dialog(Dialog.Kind.SAVE, "Save game", textFieldValue = longName, buttons = saveButtons, textFieldFocused = true),
        )

        val frame = renderer.render(state, terminalSize)

        assertTrue(frame.contains("Name: ${longName}_ ◀"))
        assertFalse(frame.contains("…"))
    }
}
