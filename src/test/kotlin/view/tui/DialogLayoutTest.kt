package view.tui

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Covers GH-3's dialog geometry: the box must be wide enough to fit whatever it's asked to
 * show - a long overwrite warning, a long save name - rather than a fixed width that content
 * can overflow past the right border (reported against the Save dialog's overwrite warning).
 */
class DialogLayoutTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)

    @Test
    fun `the box widens to fit a message longer than the default width`() {
        val longMessage = "'current_save' already exists - it will be overwritten."
        val dialog = Dialog(
            kind = Dialog.Kind.SAVE,
            title = "Save game",
            message = longMessage,
            textFieldValue = "current_save",
            buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
        )

        val layout = DialogLayout(dialog, terminalSize)

        // The message is drawn starting at left + 2 - the box's right edge (left + width) must
        // clear the message's last column, or it renders past the border.
        assertTrue(layout.left + 2 + longMessage.length <= layout.left + layout.width)
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
}
