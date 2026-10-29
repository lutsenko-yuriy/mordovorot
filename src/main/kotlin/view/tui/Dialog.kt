package view.tui

/**
 * One data model for GH-3's three modal dialogs (Save/Load/Exit), the Load-shaped startup
 * restore prompt, and GH-44's size picker, so [ScreenRenderer]/[TuiView] don't need one
 * near-identical class per dialog. Which fields are populated depends on [kind]:
 * [textFieldValue] is Save-only, [listItems] and [selectedIndex] are Load and LABELS (GH-139) only. SIZE,
 * UNSOLVABLE (GH-74) and GIVE_UP (GH-126) are buttons-only, like EXIT - they need no fields of their own.
 */
data class Dialog(
    val kind: Kind,
    val title: String,
    val message: String? = null,
    val textFieldValue: String = "",
    val listItems: List<String> = emptyList(),
    val selectedIndex: Int = -1,
    val buttons: List<DialogButtonSpec>,
    /** The [DialogButtonSpec.id] of the button focus is currently on in keyboard mode (GH-18),
     *  or `null` in mouse mode / whenever focus is elsewhere in the dialog. Drawn reverse video
     *  by [DialogRenderer.draw]. */
    val focusedButtonId: String? = null,
    /** Whether keyboard focus (GH-18) is on the Save dialog's text field - drives the focus
     *  marker [DialogRenderer.draw] appends after the field's caret. Always `false` for
     *  non-Save dialogs and in mouse mode. */
    val textFieldFocused: Boolean = false,
    /** [listItems] indices drawn dimmed that can't be clicked or focused (LABELS, GH-160). */
    val disabledRows: Set<Int> = emptySet(),
) {
    enum class Kind {
        SAVE, LOAD, EXIT, SIZE, UNSOLVABLE, GIVE_UP, LABELS;

        /** Whether the dialog has selectable list rows. */
        val hasList: Boolean get() = this == LOAD || this == LABELS
    }
}

/** One dialog button: [id] is what [HitTarget.DialogButton] carries back on a click, [label]
 *  is the text drawn inside its `[ ]` brackets. */
data class DialogButtonSpec(val id: String, val label: String)
