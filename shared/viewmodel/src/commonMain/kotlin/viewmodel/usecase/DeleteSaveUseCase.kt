package viewmodel.usecase

import analytics.AnalyticsService
import storage.SaveRepository
import viewmodel.CurrentSaveName
import viewmodel.DeleteResult
import viewmodel.SaveCatalog
import viewmodel.SaveInfo
import viewmodel.analyticsState

/** Deletes one save and tracks `save_deleted` (GH-251). Not `suspend` and never raises a
 *  `UiRequest`, so a View can call it while it answers `ConfirmRestore` or `ChooseSaveToRestore`
 *  (a request handler must not call a method that raises a request, or it waits on itself). */
internal class DeleteSaveUseCase(
    private val saves: SaveRepository,
    private val catalog: SaveCatalog,
    private val analytics: AnalyticsService,
    private val currentSaveName: CurrentSaveName,
) {

    /** [trigger] is `"command"` (the Load dialog) or `"startup_prompt"` (the restore dialog), as
     *  for `load_command_used`. No CancellationException guard: nothing here suspends, so the
     *  catch-all can't see one (same reasoning as [SaveCatalog]). */
    fun deleteSave(save: SaveInfo, trigger: String): DeleteResult {
        val base = mapOf("trigger" to trigger, "save_state" to save.analyticsState())
        val deleted = try {
            saves.delete(save.name)
        } catch (e: Exception) {
            analytics.track("save_deleted", base + ("result" to "error"))
            return DeleteResult.ERROR
        }
        if (!deleted) {
            // Gone already (another tab): it can't be the save the Save dialog should prefill.
            if (currentSaveName.name == save.name) currentSaveName.name = null
            analytics.track("save_deleted", base + ("result" to "not_found"))
            return DeleteResult.NOT_FOUND
        }
        val wasCurrent = currentSaveName.name == save.name
        if (wasCurrent) currentSaveName.name = null
        val remaining = try {
            catalog.count()
        } catch (e: Exception) {
            null
        }
        analytics.track(
            "save_deleted",
            base + mapOf("result" to "success", "was_current_save" to wasCurrent) +
                (remaining?.let { mapOf("remaining_save_count" to it) } ?: emptyMap()),
        )
        return DeleteResult.DELETED
    }
}
