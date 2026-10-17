package view.tui.dialog

import analytics.AnalyticsService
import view.tui.Dialog
import view.tui.DialogButtonSpec
import view.tui.HitTarget
import view.tui.InputAction
import view.tui.TuiScreen
import viewmodel.ViewModel

/** The Save dialog (GH-78): the toolbar's direct save and the exit flow's name prompt. */
internal class SaveDialogController(
    private val screen: TuiScreen,
    private val analytics: AnalyticsService,
    private val viewModel: ViewModel,
) {

    suspend fun onToolbar() {
        when (val outcome = runSaveDialog("toolbar")) {
            is SaveOutcome.Confirm -> {
                // Same no-whitespace rule as the exit flow's promptForValidSaveName (GH-79 D6) -
                // toolbar Save calls saveGame directly, bypassing that
                // validation (audit round 5 on PR #24).
                if (outcome.name.any { it.isWhitespace() }) {
                    screen.showMessage("'${outcome.name}' isn't a usable save name (no spaces) - not saved.")
                } else {
                    viewModel.saveGame(outcome.name)
                }
            }
            SaveOutcome.Cancel -> {}
        }
    }

    private sealed class SaveOutcome {
        data class Confirm(val name: String) : SaveOutcome()
        object Cancel : SaveOutcome()
    }

    /** Runs the Save dialog's own blocking loop: typed name via key events, Save/Cancel via
     *  click. Shared by the toolbar's direct save and [promptSaveName] (the exit flow). */
    private fun runSaveDialog(openedFrom: String): SaveOutcome {
        // Prefilled with the current save name (GH-99), caret at the end.
        var typed = viewModel.currentSaveName() ?: ""
        analytics.track("screen_save_dialog", mapOf("opened_from" to openedFrom, "prefilled" to typed.isNotEmpty()))
        // Consumed once, then kept sticky for the whole dialog session - clearing it every
        // iteration made the exit flow's invalid-name explanation vanish after the user's very
        // first keystroke (audit round 2 on PR #24). Shown before any overwrite warning, which
        // a prefill makes live from the start (GH-99).
        val initialMessage = screen.takeMessage()
        val dialog = {
            Dialog(
                kind = Dialog.Kind.SAVE,
                title = "Save game",
                message = listOfNotNull(initialMessage, overwriteWarning(typed)).joinToString(" ").ifEmpty { null },
                textFieldValue = typed,
                buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
            )
        }
        return screen.runModal("save", cancelled = SaveOutcome.Cancel, dialog = dialog) { action ->
            when (action) {
                is InputAction.TextChar -> typed += action.char
                InputAction.EraseChar -> typed = typed.dropLast(1)
                // An empty name means "skip saving" - Confirm("") would re-prompt forever
                // (audit round 2 on PR #24).
                InputAction.Submit -> return if (typed.isEmpty()) SaveOutcome.Cancel else SaveOutcome.Confirm(typed)
                is InputAction.Activate ->
                    if (action.target == HitTarget.DialogButton("save")) return if (typed.isEmpty()) SaveOutcome.Cancel else SaveOutcome.Confirm(typed)
                else -> {}
            }
        }
    }

    private fun overwriteWarning(name: String): String? =
        if (name.isNotEmpty() && viewModel.saveExists(name)) "'$name' already exists - it will be overwritten." else null

    suspend fun promptSaveName(): String? =
        when (val outcome = runSaveDialog("exit_flow")) {
            is SaveOutcome.Confirm -> outcome.name
            SaveOutcome.Cancel -> null
        }
}
