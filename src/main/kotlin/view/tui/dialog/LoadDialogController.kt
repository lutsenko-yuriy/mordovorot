package view.tui.dialog

import analytics.AnalyticsService
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.SaveInfo
import viewmodel.ViewModel
import viewmodel.display

/** The Load-shaped dialog (GH-78): the toolbar's Load, and the startup restore prompt through
 *  [RestoreDialogController]. */
internal class LoadDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
    private val viewModel: ViewModel,
) {

    suspend fun onToolbar() {
        when (val outcome = runLoadDialog("Load game", "toolbar")) {
            is LoadOutcome.Confirm -> viewModel.loadGame(outcome.name)
            LoadOutcome.Cancel -> {}
        }
    }

    sealed class LoadOutcome {
        data class Confirm(val name: String) : LoadOutcome()
        object Cancel : LoadOutcome()
    }

    /** Runs a Load-shaped dialog's own blocking loop: click a list row to select, Load/Cancel
     *  via click. Shared by the toolbar's Load and the startup restore prompt (same shape for
     *  any save count, per the plan). [preloadedSaves], when given, is shown as-is instead of
     *  a fresh [ViewModel.listSaves] call - the restore prompt already receives the save list
     *  [viewmodel.usecase.StartupRestoreUseCase.restoreOnStartup] queried, and re-querying
     *  instead risked disagreeing with it (audit finding on PR #24). Each row shows its size
     *  (GH-44 WU4) via [display]. */
    fun runLoadDialog(title: String, openedFrom: String, preloadedSaves: List<SaveInfo>? = null): LoadOutcome {
        val saves = preloadedSaves ?: viewModel.listSaves()
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
        var selected = if (saves.isNotEmpty()) 0 else -1
        val dialog = {
            Dialog(
                kind = Dialog.Kind.LOAD,
                title = title,
                message = if (saves.isEmpty()) "No saves found." else null,
                listItems = saves.map { it.display() },
                selectedIndex = selected,
                buttons = listOf(DialogButtonSpec("load", "Load"), DialogButtonSpec("cancel", "Cancel")),
            )
        }
        return screen.runModal("load", cancelled = LoadOutcome.Cancel, dialog = dialog) { action ->
            when (val target = (action as? InputAction.Activate)?.target) {
                is HitTarget.DialogListRow -> selected = target.index
                HitTarget.DialogButton("load") -> if (selected in saves.indices) return LoadOutcome.Confirm(saves[selected].name)
                else -> {}
            }
        }
    }
}
