package view.tui

/** Fixed interior width for every dialog box - wide enough for the widest content (typed save
 *  names, save-list rows) that isn't a free-form message. */
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

    // The box's width is driven by its "structural" content (title, text field, list rows,
    // buttons) - not the free-form message, which wraps to fit instead of growing the box.
    // Growing the box to a long message's raw length was still getting capped by the terminal
    // width and truncated, which could cut off the actionable half of a multi-clause message
    // (audit round 3 on PR #24: the exit flow's invalid-name explanation lost its "press Enter
    // to skip saving" half at 80 columns). The cap floors at a small constant, not DIALOG_WIDTH -
    // flooring it at DIALOG_WIDTH defeated the cap entirely on any terminal narrower than
    // DIALOG_WIDTH, which is exactly the terminal size this cap exists to protect (audit round 2).
    val width = maxOf(DIALOG_WIDTH, structuralContentWidth(dialog) + 4)
        .coerceAtMost((terminalSize.columns - 4).coerceAtLeast(MIN_WIDTH))

    private val hasList = dialog.kind == Dialog.Kind.LOAD
    private val listRows = if (hasList) dialog.listItems.size.coerceAtLeast(1) else 0
    private val hasTextField = dialog.kind == Dialog.Kind.SAVE

    /** The message word-wrapped to fit the chosen [width] - possibly several lines, unlike
     *  every other content line. */
    val messageLines: List<String> = dialog.message?.takeIf { it.isNotEmpty() }
        ?.let { wordWrap(it, (width - 4).coerceAtLeast(1)) } ?: emptyList()

    private val height = 2 + // title + blank
        messageLines.size +
        (if (hasTextField) 1 else 0) +
        listRows +
        2 // blank + buttons

    val left = ((terminalSize.columns - width) / 2).coerceAtLeast(0)
    val top = ((terminalSize.rows - height) / 2).coerceAtLeast(0)

    // Rows below the title+blank are assigned sequentially: message (however many lines it
    // wrapped to), then text field, then the list - each only if the dialog actually has one.
    private val messageStartRow: Int? = if (messageLines.isNotEmpty()) top + 2 else null
    // messageStartRow + messageLines.size - 1 is the *last* message row, matching the "last
    // used row" the `?: top + 1` fallback beside it returns - the earlier `+ messageLines.size`
    // (with no `- 1`) pointed one row past the last message line instead, leaving a spurious
    // blank row before whatever follows (audit round 4 on PR #24).
    val textFieldRow: Int? = if (hasTextField) (messageStartRow?.plus(messageLines.size - 1) ?: top + 1) + 1 else null
    private val listStartRow = (textFieldRow ?: messageStartRow?.plus(messageLines.size - 1) ?: top + 1) + 1
    private val buttonsRow = listStartRow + listRows + 1

    fun titleRow(): Int = top
    fun bottomRow(): Int = buttonsRow
    fun buttonsRow(): Int = buttonsRow
    fun messageRowPosition(index: Int): Int = (messageStartRow ?: top + 2) + index
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

/** The widest single line the dialog's non-message content needs: title, the `Name: <value>_`
 *  text field, the longest list row, or the button row - whichever is longest. The message is
 *  deliberately excluded - it wraps to fit [DialogLayout.width] instead of driving it. */
private fun structuralContentWidth(dialog: Dialog): Int {
    val lines = mutableListOf(dialog.title.length)
    if (dialog.kind == Dialog.Kind.SAVE) lines += "Name: ${dialog.textFieldValue}_".length
    dialog.listItems.forEach { lines += "  $it".length }
    val buttonsWidth = dialog.buttons.sumOf { "[ ${it.label} ]".length } + (dialog.buttons.size - 1)
    lines += buttonsWidth
    return lines.max()
}

/** Greedy word-wrap: packs whole words onto a line up to [maxWidth], breaking to a new line
 *  rather than truncating - unlike every other content line, a message can be long enough that
 *  cutting it off would hide the actionable half of a multi-clause sentence. A single word
 *  longer than [maxWidth] is hard-broken across as many lines as it needs (not truncated - the
 *  same "don't hide content" reasoning applies to one long word, e.g. a save name in the
 *  overwrite warning, as to the message overall). */
private fun wordWrap(text: String, maxWidth: Int): List<String> {
    val lines = mutableListOf<String>()
    var current = StringBuilder()
    for (word in text.split(" ")) {
        val candidate = if (current.isEmpty()) word else "$current $word"
        when {
            candidate.length <= maxWidth -> current = StringBuilder(candidate)
            word.length > maxWidth -> {
                if (current.isNotEmpty()) lines += current.toString()
                lines += word.chunked(maxWidth)
                current = StringBuilder()
            }
            else -> {
                lines += current.toString()
                current = StringBuilder(word)
            }
        }
    }
    if (current.isNotEmpty() || lines.isEmpty()) lines += current.toString()
    return lines
}
