package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** GH-74: three buttons are wider than the default box - it grows, and every button stays
     *  inside the border and hit-testable at 80 columns. */
    @Test
    fun `the unsolvable dialog's three buttons fit inside the box at 80 columns`() {
        val dialog = Dialog(
            Dialog.Kind.UNSOLVABLE,
            "Can't be solved",
            message = "'odd (3x3)' can't be solved by any sequence of shifts.",
            buttons = listOf(DialogButtonSpec("repair", "Repair & load"), DialogButtonSpec("new", "New game"), DialogButtonSpec("exit", "Exit")),
        )

        val layout = DialogLayout(dialog, TerminalSize(80, 24))

        val buttons = layout.buttons()
        assertEquals(3, buttons.size)
        assertTrue(buttons.all { it.range.first > layout.left && it.range.last < layout.left + layout.width - 1 }, "$buttons")
        assertTrue(layout.left + layout.width <= 80)
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

    @Test
    fun `a Labels dialog's rows hit-test like a Load dialog's (GH-139)`() {
        val dialog = Dialog(
            kind = Dialog.Kind.LABELS,
            title = "Tile labels",
            listItems = List(6) { "row $it" },
            selectedIndex = 0,
            buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
        )
        val layout = DialogLayout(dialog, terminalSize)

        for (index in 0 until 6) assertEquals(HitTarget.DialogListRow(index), layout.hitTest(layout.left + 2, layout.listRowPosition(index)))
        assertEquals(layout.listRowPosition(5) + 2, layout.buttonsRow())
    }

    @Test
    fun `a disabled Labels row hits nothing (GH-160)`() {
        val dialog = Dialog(
            kind = Dialog.Kind.LABELS,
            title = "Tile labels",
            listItems = List(6) { "row $it" },
            buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
            disabledRows = setOf(4),
        )
        val layout = DialogLayout(dialog, terminalSize)

        assertEquals(HitTarget.Nothing, layout.hitTest(layout.left + 2, layout.listRowPosition(4)))
        assertEquals(HitTarget.DialogListRow(5), layout.hitTest(layout.left + 2, layout.listRowPosition(5)))
    }
}
