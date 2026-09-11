package view.tui

/** Fixed interior width for every dialog box - wide enough for the widest content (typed save
 *  names, the overwrite warning, save-list rows). */
internal const val DIALOG_WIDTH = 44

/** Absolute floor for the terminal-width cap below - small enough to still fit inside a
 *  pathologically narrow terminal rather than forcing overflow. */
private const val MIN_WIDTH = 12

/**
 * Pure geometry for GH-3's modal dialogs: given a [Dialog] and the terminal size, computes
 * where its title, message, text field, list rows, and buttons land, and resolves a click to a
 * [HitTarget]. Mirrors [BoardLayout]'s draw/hit-test coupling - [ScreenRenderer] draws to these
 * same coordinates.
 */
class DialogLayout(private val dialog: Dialog, terminalSize: TerminalSize) {

    // Widens past the default to fit whatever the dialog is actually showing (a long overwrite
    // warning, a long typed name, a long save name) - a fixed width let content overflow past
    // the right border instead. left+2 is the fixed left margin every content line is drawn at
    // (see ScreenRenderer.drawDialog); +2 more for the same margin on the right. The cap floors
    // at a small constant, not DIALOG_WIDTH - flooring it at DIALOG_WIDTH defeated the cap
    // entirely on any terminal narrower than DIALOG_WIDTH, which is exactly the terminal size
    // this cap exists to protect (audit round 2 on PR #24: content was still overflowing the
    // canvas on a 40-column terminal because the box was held at 44 regardless).
    val width = maxOf(DIALOG_WIDTH, contentWidth(dialog) + 4).coerceAtMost((terminalSize.columns - 4).coerceAtLeast(MIN_WIDTH))
    private val hasList = dialog.kind == Dialog.Kind.LOAD
    private val listRows = if (hasList) dialog.listItems.size.coerceAtLeast(1) else 0
    private val hasTextField = dialog.kind == Dialog.Kind.SAVE
    private val hasMessage = dialog.message != null

    private val height = 2 + // title + blank
        (if (hasMessage) 1 else 0) +
        (if (hasTextField) 1 else 0) +
        listRows +
        2 // blank + buttons

    val left = ((terminalSize.columns - width) / 2).coerceAtLeast(0)
    val top = ((terminalSize.rows - height) / 2).coerceAtLeast(0)

    // Rows below the title+blank are assigned sequentially: message, then text field, then
    // the list - each only if the dialog actually has one.
    val messageRow: Int? = if (hasMessage) top + 2 else null
    val textFieldRow: Int? = if (hasTextField) (messageRow ?: top + 1) + 1 else null
    private val listStartRow = (textFieldRow ?: messageRow ?: top + 1) + 1
    private val buttonsRow = listStartRow + listRows + 1

    fun titleRow(): Int = top
    fun bottomRow(): Int = buttonsRow
    fun buttonsRow(): Int = buttonsRow
    fun listRowPosition(index: Int): Int = listStartRow + index

    internal fun buttons(): List<DialogButtonLayout> {
        val texts = dialog.buttons.map { "[ ${it.label} ]" }
        val totalWidth = texts.sumOf { it.length } + (texts.size - 1)
        var x = left + ((width - totalWidth) / 2).coerceAtLeast(0)
        return dialog.buttons.mapIndexed { index, button ->
            val text = texts[index]
            val entry = DialogButtonLayout(HitTarget.DialogButton(button.id), text, x, x until (x + text.length))
            x += text.length + 1
            entry
        }
    }

    fun hitTest(x: Int, y: Int): HitTarget {
        if (hasList && dialog.listItems.isNotEmpty() && x in left until (left + width)) {
            for (index in dialog.listItems.indices) if (y == listRowPosition(index)) return HitTarget.DialogListRow(index)
        }
        if (y == buttonsRow) {
            for (button in buttons()) if (x in button.range) return button.target
        }
        return HitTarget.Nothing
    }
}

internal data class DialogButtonLayout(val target: HitTarget, val text: String, val x: Int, val range: IntRange)

/** The widest single line the dialog needs to show: title, message, the `Name: <value>_` text
 *  field, the longest list row, or the button row - whichever is longest. */
private fun contentWidth(dialog: Dialog): Int {
    val lines = mutableListOf(dialog.title.length)
    dialog.message?.let { lines += it.length }
    if (dialog.kind == Dialog.Kind.SAVE) lines += "Name: ${dialog.textFieldValue}_".length
    dialog.listItems.forEach { lines += "  $it".length }
    val buttonsWidth = dialog.buttons.sumOf { "[ ${it.label} ]".length } + (dialog.buttons.size - 1)
    lines += buttonsWidth
    return lines.max()
}
