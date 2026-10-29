package view.tui

/** Appended to the Save dialog's text field when keyboard focus (GH-18) is on it. Internal,
 *  not private - [DialogLayout.structuralContentWidth] reserves room for it unconditionally so
 *  the box doesn't resize (and truncate the caret) the moment focus lands on the field. */
internal const val TEXT_FIELD_FOCUS_MARKER = " ◀"

/** Draws a [Dialog] over the board (GH-3), at the coordinates [DialogLayout] hit-tests against.
 *  Split out of [ScreenRenderer] (GH-170). */
internal object DialogRenderer {

    fun draw(canvas: Canvas, dialog: Dialog, terminalSize: TerminalSize) {
        val layout = DialogLayout(dialog, terminalSize)
        // DialogLayout.width can be capped below what the content actually needs (a terminal
        // too narrow to fit it in full) - truncating here is what keeps that content from
        // overwriting the box's own right border (audit round 2 on PR #24).
        val maxLineWidth = (layout.width - 4).coerceAtLeast(1)
        drawDialogBox(canvas, layout)
        canvas.put(layout.left + 2, layout.titleRow(), truncate(dialog.title, maxLineWidth))
        // The message is already word-wrapped to fit maxLineWidth (DialogLayout.messageLines) -
        // drawn one line per row, not truncated, so a long multi-clause message (e.g. the exit
        // flow's invalid-name explanation) doesn't lose its actionable half (audit round 3 on
        // PR #24).
        layout.messageLines.forEachIndexed { index, line -> canvas.put(layout.left + 2, layout.messageRowPosition(index), line) }
        layout.textFieldRow?.let { row ->
            val focusMarker = if (dialog.textFieldFocused) TEXT_FIELD_FOCUS_MARKER else ""
            canvas.put(layout.left + 2, row, truncate("Name: ${dialog.textFieldValue}_$focusMarker", maxLineWidth))
        }
        for (index in dialog.listItems.indices) {
            val selected = index == dialog.selectedIndex
            val marker = when {
                dialog.kind == Dialog.Kind.LABELS -> if (selected) "● " else "○ "
                selected -> "> "
                else -> "  "
            }
            val text = truncate("$marker${dialog.listItems[index]}", maxLineWidth)
            if (index in dialog.disabledRows) canvas.putColored(layout.left + 2, layout.listRowPosition(index), text, DIM_ON, DIM_OFF)
            else canvas.put(layout.left + 2, layout.listRowPosition(index), text)
        }
        val focusedTarget = dialog.focusedButtonId?.let { HitTarget.DialogButton(it) }
        for (button in layout.buttons()) {
            if (button.target == focusedTarget) canvas.putHighlighted(button.x, layout.buttonsRow(), button.text)
            else canvas.put(button.x, layout.buttonsRow(), button.text)
        }
    }

    private fun truncate(text: String, maxWidth: Int): String =
        if (text.length <= maxWidth) text else text.take((maxWidth - 1).coerceAtLeast(0)) + "…"

    private fun drawDialogBox(canvas: Canvas, layout: DialogLayout) {
        val top = layout.titleRow() - 1
        val bottom = layout.bottomRow() + 1
        val left = layout.left - 1
        val boxWidth = layout.width + 2
        canvas.put(left, top, "┌" + "─".repeat(boxWidth - 2) + "┐")
        for (row in (top + 1) until bottom) canvas.put(left, row, "│" + " ".repeat(boxWidth - 2) + "│")
        canvas.put(left, bottom, "└" + "─".repeat(boxWidth - 2) + "┘")
    }
}
