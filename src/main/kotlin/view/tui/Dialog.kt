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
) {
    enum class Kind { SAVE, LOAD, EXIT }
}

/** One dialog button: [id] is what [HitTarget.DialogButton] carries back on a click, [label]
 *  is the text drawn inside its `[ ]` brackets. */
data class DialogButtonSpec(val id: String, val label: String)
