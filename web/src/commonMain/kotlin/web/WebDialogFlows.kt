package web

import analytics.AnalyticsService
import board_model.BoardSize
import viewmodel.GameViewModel
import viewmodel.SaveInfo
import viewmodel.UnsolvableSaveChoice
import viewmodel.display

/** [WebGame]'s Save, Load, size and can't-be-solved dialogs, each tracking its screen view and
 *  Cancel. [modal] shows them; [cancelled] tracks a Cancel. */
internal class WebDialogFlows(
    private val viewModel: GameViewModel,
    private val analytics: AnalyticsService,
    private val modal: WebModal,
    private val cancelled: (dialog: String) -> Unit,
) {

    /** [openedFrom] is `toolbar` or a `ChooseBoardSize.origin`, as `screen_size_dialog.opened_from`. */
    suspend fun chooseSize(openedFrom: String): Int? {
        analytics.track("screen_size_dialog", mapOf("opened_from" to openedFrom))
        val size = modal.ask<Int?>(WebDialog.Size((BoardSize.MIN..BoardSize.MAX).toList(), viewModel.squareSide()))
        if (size == null) cancelled("size")
        return size
    }

    /** The startup restore offer, for one save or a pick list; the chosen save's name or `null`. */
    suspend fun chooseRestore(saves: List<SaveInfo>): String? = chooseSave(saves, restore = true)

    suspend fun chooseSave(saves: List<SaveInfo>, restore: Boolean): String? {
        val openedFrom = if (restore) "startup" else "toolbar"
        analytics.track("screen_load_dialog", mapOf("opened_from" to openedFrom, "save_file_count" to saves.size))
        val name = modal.ask<String?>(WebDialog.Load(restore, saves.map { SaveEntry(it.name, it.display()) }))
        if (name == null) cancelled("load")
        return name
    }

    /** The Save dialog prefilled with the current save's name; the typed name, or `null`. */
    suspend fun askSaveName(): String? {
        val prefill = viewModel.currentSaveName() ?: ""
        analytics.track("screen_save_dialog", mapOf("opened_from" to "toolbar", "prefilled" to prefill.isNotEmpty()))
        val name = modal.ask<String?>(saveDialog(prefill))
        if (name == null) cancelled("save")
        // A blank name is Cancel, untracked like the terminal's empty Save.
        return name?.takeUnless { it.isBlank() }
    }

    fun saveDialog(name: String) = WebDialog.Save(
        name,
        if (name.isNotEmpty() && viewModel.saveExists(name)) "'$name' already exists - it will be overwritten." else null,
    )

    /** [openedFrom] is `startup` or `toolbar`, as `screen_unsolvable_dialog.opened_from`. */
    suspend fun resolveUnsolvable(message: String, openedFrom: String): UnsolvableSaveChoice {
        analytics.track("screen_unsolvable_dialog", mapOf("opened_from" to openedFrom))
        return modal.ask(WebDialog.Unsolvable(message))
    }
}
