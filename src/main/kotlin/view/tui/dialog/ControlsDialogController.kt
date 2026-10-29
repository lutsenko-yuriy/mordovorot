package view.tui.dialog

import InputMode
import view.tui.BoardKeys
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.DialogOptionGroup
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.ModeSwitcher
import viewmodel.ViewModel

/** The Controls dialog (GH-170): `[ Controls 6 ]` / 6 in both modes. `Play with` marks the
 *  current mode, then the key list; OK with the other mode marked switches to it,
 *  OK with the current one is Cancel without its event. */
internal class ControlsDialogController(
    private val screen: TuiScreen,
    private val viewModel: ViewModel,
    private val modeSwitcher: ModeSwitcher,
) {

    /** [openedFrom] is `toolbar` (a click) or `shortcut` (a key). */
    suspend fun onToolbar(openedFrom: String) {
        viewModel.controlsDialogOpened(openedFrom)
        val mode = runControlsDialog() ?: return
        if (mode != modeSwitcher.currentMode) modeSwitcher.switchTo(mode, trigger = "controls_dialog")
    }

    /** The marked mode on OK, `null` on Cancel / Escape / end of input. */
    private fun runControlsDialog(): InputMode? {
        val modes = InputMode.entries
        var selected = modes.indexOf(modeSwitcher.currentMode)
        val dialog = {
            Dialog(
                kind = Dialog.Kind.CONTROLS,
                title = "Controls",
                optionGroups = listOf(DialogOptionGroup("Play with", modes.map { it.title }, selected)),
                notes = BoardKeys.KEY_LIST,
                buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
            )
        }
        return screen.runModal<InputMode?>("controls", cancelled = null, dialog = dialog) { action ->
            if (action == InputAction.Submit) return modes[selected]
            when (val target = (action as? InputAction.Activate)?.target) {
                is HitTarget.DialogOption -> selected = target.option
                HitTarget.DialogButton("ok") -> return modes[selected]
                else -> {}
            }
        }
    }

    private val InputMode.title get() = name.lowercase().replaceFirstChar { it.uppercase() }
}
