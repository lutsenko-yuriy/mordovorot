package view.tui

/**
 * One data model for GH-3's three modal dialogs (Save/Load/Exit) and the Load-shaped startup
 * restore prompt, so [ScreenRenderer]/[TuiView] don't need three near-identical classes. Which
 * fields are populated depends on [kind]: [textFieldValue] is Save-only, [listItems] and
 * [selectedIndex] are Load-only.
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
     *  by [ScreenRenderer.drawDialog]. */
    val focusedButtonId: String? = null,
    /** Whether keyboard focus (GH-18) is on the Save dialog's text field - drives the focus
     *  marker [ScreenRenderer.drawDialog] appends after the field's caret. Always `false` for
     *  non-Save dialogs and in mouse mode. */
    val textFieldFocused: Boolean = false,
) {
    enum class Kind { SAVE, LOAD, EXIT }
}

/** One dialog button: [id] is what [HitTarget.DialogButton] carries back on a click, [label]
 *  is the text drawn inside its `[ ]` brackets. */
data class DialogButtonSpec(val id: String, val label: String)
