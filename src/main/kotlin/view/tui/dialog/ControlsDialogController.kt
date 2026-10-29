package view.tui.dialog

import InputMode
import view.tui.BoardKeys
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogOptionGroup
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.KeyboardLayout
import viewmodel.ModeSwitcher
import viewmodel.SessionKeyboardLayout
import viewmodel.ViewModel

/** The Controls dialog (GH-170): `[ Controls 6 ]` / 6 in both modes. `Play with` marks the
 *  current mode, `Keyboard keys` the current layout, then the key list of the marked layout.
 *  OK applies the layout, then switches mode if the other one is marked; OK changing nothing is
 *  Cancel without its event. */
internal class ControlsDialogController(
    private val screen: TuiScreen,
    private val viewModel: ViewModel,
    private val modeSwitcher: ModeSwitcher,
    private val keyboardLayout: SessionKeyboardLayout,
) {

    /** [openedFrom] is `toolbar` (a click) or `shortcut` (a key). */
    suspend fun onToolbar(openedFrom: String) {
        viewModel.controlsDialogOpened(openedFrom)
        val (mode, layout) = runControlsDialog() ?: return
        keyboardLayout.choose(layout)
        if (mode != modeSwitcher.currentMode) modeSwitcher.switchTo(mode, trigger = "controls_dialog")
    }

    /** The marked mode and layout on OK, `null` on Cancel / Escape / end of input. */
    private fun runControlsDialog(): Pair<InputMode, KeyboardLayout>? {
        val modes = InputMode.entries
        val layouts = KeyboardLayout.entries
        val selected = intArrayOf(modes.indexOf(modeSwitcher.currentMode), layouts.indexOf(keyboardLayout.current))
        val chosen = { modes[selected[0]] to layouts[selected[1]] }
        val dialog = {
            Dialog(
                kind = Dialog.Kind.CONTROLS,
                title = "Controls",
                optionGroups = listOf(
                    DialogOptionGroup("Play with", modes.map { it.title }, selected[0]),
                    DialogOptionGroup("Keyboard keys", layouts.map { it.title }, selected[1]),
                ),
                notes = BoardKeys.keyList(layouts[selected[1]]),
                buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
            )
        }
        return screen.runModal<Pair<InputMode, KeyboardLayout>?>("controls", cancelled = null, dialog = dialog) { action ->
            if (action == InputAction.Submit) return chosen()
            when (val target = (action as? InputAction.Activate)?.target) {
                is HitTarget.DialogOption -> selected[target.group] = target.option
                HitTarget.DialogButton("ok") -> return chosen()
                else -> {}
            }
        }
    }

    private val InputMode.title get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
