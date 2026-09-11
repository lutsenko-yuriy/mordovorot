package view.tui

/** Fixed interior width for every dialog box - wide enough for the widest content (typed save
 *  names, the overwrite warning, save-list rows). */
internal const val DIALOG_WIDTH = 44

/**
 * Pure geometry for GH-3's modal dialogs: given a [Dialog] and the terminal size, computes
 * where its title, message, text field, list rows, and buttons land, and resolves a click to a
 * [HitTarget]. Mirrors [BoardLayout]'s draw/hit-test coupling - [ScreenRenderer] draws to these
 * same coordinates.
 */
class DialogLayout(private val dialog: Dialog, terminalSize: TerminalSize) {

    val width = DIALOG_WIDTH
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
