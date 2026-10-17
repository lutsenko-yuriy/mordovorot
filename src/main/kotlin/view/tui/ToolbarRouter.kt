package view.tui

import view.tui.dialog.ExitDialogController
import view.tui.dialog.LoadDialogController
import view.tui.dialog.SaveDialogController
import view.tui.dialog.SizeDialogController
import viewmodel.ModeSwitcher

/** Runs the toolbar button (or its keyboard shortcut) the player activated (GH-78). */
internal class ToolbarRouter(
    private val saveDialog: SaveDialogController,
    private val loadDialog: LoadDialogController,
    private val exitDialog: ExitDialogController,
    private val sizeDialog: SizeDialogController,
    private val modeSwitcher: ModeSwitcher,
    private val input: TuiInput,
) {

    /** Returns whether [target] was a toolbar button. */
    suspend fun handle(target: HitTarget): Boolean {
        when (target) {
            HitTarget.ToolbarSave -> saveDialog.onToolbar()
            HitTarget.ToolbarLoad -> loadDialog.onToolbar()
            HitTarget.ToolbarExit -> exitDialog.onToolbar()
            HitTarget.ToolbarNew -> sizeDialog.onToolbar()
            is HitTarget.ToolbarMode -> modeSwitcher.switchTo(target.mode, trigger = input.switchTrigger)
            else -> return false
        }
        return true
    }
}
