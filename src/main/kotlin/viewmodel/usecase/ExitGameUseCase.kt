package viewmodel.usecase

import analytics.AnalyticsService
import storage.SaveRepository
import viewmodel.ExitRequestedException
import viewmodel.UiRequest
import viewmodel.UiRequestChannel

/** Ends the session from the toolbar Exit, offering to save first. */
internal class ExitGameUseCase(
    private val saves: SaveRepository,
    private val saveGame: SaveGameUseCase,
    private val analytics: AnalyticsService,
    private val ui: UiRequestChannel,
) {

    /**
     * Ends the current session on demand (toolbar Exit), asking whether to
     * save first. Declining, or a blank/EOF name, ends the session unsaved. A save *attempt*
     * that fails does **not** end the session (see below) - [exitGame] can return normally
     * instead of always throwing [ExitRequestedException].
     */
    suspend fun exitGame() {
        if (!ui.ask(UiRequest.ConfirmSaveBeforeExit())) {
            analytics.track("exit_command_used", mapOf("save_choice" to "declined"))
            throw ExitRequestedException()
        }

        val name = promptForValidSaveName()
        if (name == null) {
            analytics.track("exit_command_used", mapOf("save_choice" to "declined"))
            throw ExitRequestedException()
        }

        if (!saveGame.saveGame(name)) {
            // saveGame already showed the error via its own catch block, but that alone reads
            // like ordinary game output, not "your quit was cancelled" - say so explicitly
            // (audit round 3 on GH-12/PR #15). Quitting anyway would compound a failed save
            // with a lost session - instead, let the user try `exit` (or `save`) again once
            // the underlying issue (e.g. a read-only saves/ directory) is resolved. No
            // exit_command_used - the session didn't actually end, so "declined" would
            // misrepresent an explicit save request as a decision not to save (audit round 2).
            ui.showMessage(
                "Not quitting - your game is still running. Fix the problem and try exit again, " +
                    "or answer n to quit without saving."
            )
            return
        }

        analytics.track("exit_command_used", mapOf("save_choice" to "saved"))
        throw ExitRequestedException()
    }

    /** Re-prompts until [UiRequest.PromptSaveName] returns a usable save name, or `null` on a blank answer or EOF, meaning the user doesn't want to save.
     *  Re-prompting on *any* invalid name (rather than treating it as "don't save" or letting
     *  it fall through to [SaveGameUseCase.saveGame]'s failure path) avoids silently discarding an explicit
     *  save request over a naming mistake, and keeps `save_command_used {result: error}`
     *  meaning "the save attempt failed", not "the user mistyped" (audit rounds 2 and 3 on
     *  PR #15). Checks both:
     *  - no whitespace - a rule kept from the removed console's space-split `load <name>`
     *    (GH-79 D6);
     *  - [SaveRepository.isValidName] - blank, a path separator, or `..` would make
     *    [SaveGameUseCase.saveGame] throw, which [promptForValidSaveName]'s caller must not confuse with a
     *    genuine save failure (e.g. a read-only `saves/` directory).
     */
    private suspend fun promptForValidSaveName(): String? {
        while (true) {
            val name = ui.ask(UiRequest.PromptSaveName()) ?: return null
            if (name.any { it.isWhitespace() } || !saves.isValidName(name)) {
                ui.showMessage(
                    "'$name' isn't a usable save name (no spaces, path separators, or '..') - " +
                        "try again, or press Enter to skip saving."
                )
            } else {
                return name
            }
        }
    }
}
