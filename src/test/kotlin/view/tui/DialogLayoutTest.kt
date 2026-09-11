package view.tui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Covers GH-3's dialog geometry: the box must be wide enough to fit whatever it's asked to
 * show - a long typed name, a long save name - rather than a fixed width content can overflow
 * past the right border on. A long message wraps to multiple lines instead of widening the box
 * or being truncated (see the dedicated word-wrap test below).
 */
class DialogLayoutTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    @Test
    fun `a message longer than the box width wraps instead of overflowing the box`() {
        val longMessage = "'current_save' already exists - it will be overwritten."
        val dialog = Dialog(
            kind = Dialog.Kind.SAVE,
            title = "Save game",
            message = longMessage,
            textFieldValue = "current_save",
            buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
        )

        val layout = DialogLayout(dialog, terminalSize)

        // Every wrapped line must fit inside the box - none may run past the right border.
        assertTrue(layout.messageLines.all { layout.left + 2 + it.length <= layout.left + layout.width })
    }

    @Test
    fun `a short dialog keeps the default width`() {
        val dialog = Dialog(Dialog.Kind.EXIT, "Save before quitting?", buttons = listOf(DialogButtonSpec("yes", "Yes")))

        val layout = DialogLayout(dialog, terminalSize)

        assertTrue(layout.width == DIALOG_WIDTH)
    }

    @Test
    fun `the box never exceeds the terminal's own width, even on a terminal narrower than the default`() {
        // Round 2 audit finding on PR #24: flooring the terminal-width cap at DIALOG_WIDTH
        // defeated the cap on any terminal narrower than DIALOG_WIDTH, so the box (and its
        // content) still overflowed the canvas instead of being capped/truncated to fit.
        val narrow = TerminalSize(columns = 40, rows = 24)
        val dialog = Dialog(
            kind = Dialog.Kind.SAVE,
            title = "Save game",
            message = "'somesave' already exists - it will be overwritten.",
            buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
        )

        val layout = DialogLayout(dialog, narrow)

        assertTrue(layout.left + layout.width <= narrow.columns)
    }

    @Test
    fun `a long message wraps to multiple lines instead of being truncated`() {
        // Round 3 audit finding on PR #24: truncating this exact message at 80 columns cut off
        // "try again, or press Enter to skip saving" - the one instruction that tells the user
        // how to get out of the loop they're stuck in.
        val message = "'a/b' isn't a usable save name (no spaces, path separators, or '..') - " +
            "try again, or press Enter to skip saving."
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", message = message, buttons = listOf(DialogButtonSpec("save", "Save")))

        val layout = DialogLayout(dialog, TerminalSize(columns = 80, rows = 40))

        assertTrue(layout.messageLines.size > 1)
        assertTrue(layout.messageLines.joinToString(" ") == message)
        assertTrue(layout.messageLines.all { it.length <= layout.width - 4 })
    }

    @Test
    fun `a multi-line message leaves no gap before the row that follows it`() {
        // Audit round 4 on PR #24: the row math pointed one row past the last message line,
        // leaving a spurious blank row between the message and the text field below it.
        val message = "'a/b' isn't a usable save name (no spaces, path separators, or '..') - " +
            "try again, or press Enter to skip saving."
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", message = message, textFieldValue = "x", buttons = listOf(DialogButtonSpec("save", "Save")))

        val layout = DialogLayout(dialog, TerminalSize(columns = 80, rows = 40))

        assertTrue(layout.messageLines.size > 1)
        assertTrue(layout.textFieldRow == layout.top + 2 + layout.messageLines.size)
    }

    @Test
    fun `a word longer than the wrap width is hard-broken across lines instead of truncated`() {
        val longWord = "a".repeat(100)
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", message = longWord, buttons = listOf(DialogButtonSpec("save", "Save")))

        val layout = DialogLayout(dialog, TerminalSize(columns = 80, rows = 40))

        assertTrue(layout.messageLines.joinToString("") == longWord)
    }

    @Test
    fun `an empty-string message produces no message lines, same as a null message`() {
        val dialog = Dialog(Dialog.Kind.SAVE, "Save game", message = "", buttons = listOf(DialogButtonSpec("save", "Save")))

        val layout = DialogLayout(dialog, terminalSize)

        assertTrue(layout.messageLines.isEmpty())
    }
}
