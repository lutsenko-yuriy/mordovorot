package web

import analytics.AnalyticsService
import board_model.BoardSize
import viewmodel.DeleteResult
import viewmodel.GameViewModel
import viewmodel.SaveInfo
import viewmodel.UnsolvableSaveChoice
import viewmodel.analyticsState
import viewmodel.display
import viewmodel.displayWithSize
import web.a11y.Announcements

/** What the Load dialog was answered with (GH-251). */
internal sealed interface LoadPick {
    data class Chosen(val name: String) : LoadPick
    /** The trash button of the row at [index]. */
    data class Delete(val index: Int) : LoadPick
    data object Cancel : LoadPick
}

/** [WebGame]'s Save, Load, size and can't-be-solved dialogs, each tracking its screen view and
 *  Cancel. [modal] shows them; [cancelled] tracks a Cancel. */
internal class WebDialogFlows(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val cancelled: (dialog: String) -> Unit,
    /** Sets the status message and what is said, for the next frame to show. */
    private val noteStatus: (String) -> Unit,
    /** Takes the status message just shown, so a dialog can show it instead. */
    private val takeStatus: () -> String?,
    /** Sets what the next frame says. */
    private val say: (String) -> Unit,
) {

    /** The viewModel asked for a size during a flow, which so ends in a new game. */
    private var sizeAsked = false

    /** [openedFrom] is `toolbar` or a `ChooseBoardSize.origin`, as `screen_size_dialog.opened_from`. */
    suspend fun chooseSize(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val size = modal.ask<Int?>(WebDialog.Size((BoardSize.MIN..BoardSize.MAX).toList(), viewModel.squareSide(), BoardSize.VERY_LARGE.toList()))
        if (size == null) cancelled("size")
        return size
    }

    /** The startup restore offer, for one save or a pick list; the chosen save's name or `null`. */
    suspend fun chooseRestore(saves: List<SaveInfo>): String? = chooseSave(saves, restore = true)

    /** The Load dialog, shown again after each delete or cancelled delete (GH-251); the chosen
     *  save's name, or `null` when cancelled or the last save was deleted. */
    suspend fun chooseSave(saves: List<SaveInfo>, restore: Boolean): String? {
        val openedFrom = if (restore) "startup" else "toolbar"
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
        var listed = saves
        var focus: Focus? = null
        var message: String? = null
        while (true) {
            val entries = listed.map { SaveEntry(it.name, it.display()) }
            val pick = modal.ask<LoadPick>(WebDialog.Load(restore, entries, focus, message)) { it is LoadPick.Delete }
            when (pick) {
                is LoadPick.Chosen -> return pick.name
                LoadPick.Cancel -> {
                    cancelled("load")
                    return null
                }
                is LoadPick.Delete -> {
                    val save = listed[pick.index]
                    val trigger = if (restore) "startup_prompt" else "command"
                    val result = if (confirmDelete(save, listed.size, openedFrom)) viewModel.deleteSave(save, trigger) else null
                    message = when (result) {
                        null -> null
                        DeleteResult.DELETED -> "Deleted '${save.name}'."
                        DeleteResult.NOT_FOUND -> "No save named '${save.name}' - it was already deleted."
                        DeleteResult.ERROR -> "Could not delete '${save.name}'."
                    }
                    if (result == DeleteResult.DELETED) {
                        // At startup only the saves on offer when the dialog opened: StartupRestoreUseCase
                        // knows no others, so one another tab wrote since would be a pick it can't honour.
                        listed = viewModel.listSaves().filter { now -> !restore || saves.any { it.name == now.name } }
                        if (listed.isEmpty()) {
                            // Mid-game there is nothing left to load: back to the game. At startup the
                            // viewModel sees the empty list and opens the size picker.
                            noteStatus(if (restore) "Deleted '${save.name}'." else "Deleted '${save.name}'. No saves left.")
                            return null
                        }
                        // On the trash of the row that took its place: a stray second Enter then only opens another
                        // confirmation, where loading a save over the game would lose its moves.
                        focus = Focus.DeleteSave(pick.index.coerceAtMost(listed.lastIndex))
                    } else {
                        focus = Focus.DeleteSave(pick.index)
                    }
                    message?.let(noteStatus)
                }
            }
        }
    }

    /** The `Delete 'foo'?` confirmation for [save], one of [count]; whether it was confirmed. */
    private suspend fun confirmDelete(save: SaveInfo, count: Int, openedFrom: String): Boolean {
        analytics.track(
            "screen_delete_save_dialog",
            mapOf("opened_from" to openedFrom, "save_state" to save.analyticsState(), "save_file_count" to count),
        )
        val confirmed = modal.ask<Boolean>(WebDialog.DeleteSave(save.name)) { true }
        if (!confirmed) cancelled("delete_save")
        return confirmed
    }

    /** The Save dialog prefilled with the current save's name; the typed name, or `null`. */
    suspend fun askSaveName(): String? {
        val prefill = viewModel.currentSaveName() ?: ""
        analytics.track("screen_save_dialog", mapOf("opened_from" to "toolbar", "prefilled" to prefill.isNotEmpty()))
        val name = modal.ask<String?>(saveDialog(prefill))
        if (name == null) cancelled("save")
        // A blank name is Cancel, untracked.
        return name?.takeUnless { it.isBlank() }
    }

    fun saveDialog(name: String) = WebDialog.Save(
        name,
        if (name.isNotEmpty() && viewModel.saveExists(name)) "'$name' already exists - it will be overwritten." else null,
    )

    /** [openedFrom] is `startup` or `toolbar`, as `screen_unsolvable_dialog.opened_from`. */
    suspend fun resolveUnsolvable(save: SaveInfo, openedFrom: String): UnsolvableSaveChoice {
        // The viewModel's can't-be-solved message, just shown, goes in the dialog instead.
        val message = takeStatus() ?: "'${save.displayWithSize()}' can't be solved by any sequence of shifts."
        analytics.track("screen_unsolvable_dialog", mapOf("opened_from" to openedFrom))
        return modal.ask(WebDialog.Unsolvable(message))
    }

    /** The size picker the viewModel asks for, at startup or after Can't be solved. */
    suspend fun chooseNewGameSize(origin: String): Int? {
        sizeAsked = true
        return chooseSize(origin)
    }

    /** A new game once a flow in which the viewModel asked for its size ends. */
    fun sayAskedNewGame() {
        if (!sizeAsked) return
        sizeAsked = false
        say(Announcements.newGame(viewModel.squareSide()))
    }
}
