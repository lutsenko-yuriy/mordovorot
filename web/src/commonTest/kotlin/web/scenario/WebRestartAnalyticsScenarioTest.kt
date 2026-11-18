package web.scenario

import kotlin.test.Test

/** GH-349: the analytics of a restart (docs/ANALYTICS_EVENTS.md). Stubs: WU2 fills them in. */
class WebRestartAnalyticsScenarioTest {
    @Test
    fun theSaveQuestionSendsItsScreenView() {
        // TODO: 1. Make 3 shifts
        // TODO: 2. Click Restart
        // TODO: 3. The `screen_restart_save_dialog` event is sent
    }

    @Test
    fun pressingRSaysShortcut() {
        // TODO: 1. Press R
        // TODO: 2. The screen view says `shortcut`
    }

    @Test
    fun dontSaveSendsGameRestarted() {
        // TODO: 1. Choose Don't save
        // TODO: 2. The `game_restarted` event is sent
    }

    @Test
    fun aSavedRestartSendsGameRestarted() {
        // TODO: 1. Choose Save and confirm
        // TODO: 2. `game_restarted` says `saved`
    }

    @Test
    fun aWonBoardSendsNotAsked() {
        // TODO: 1. Solve, then Restart
        // TODO: 2. `game_restarted` says `not_asked` and `was_solved`
    }

    @Test
    fun cancelSendsDialogCancelled() {
        // TODO: 1. Press Cancel
        // TODO: 2. `dialog_cancelled` says `restart_save` and `button`
    }

    @Test
    fun escapeSendsDialogCancelled() {
        // TODO: 1. Press Escape
        // TODO: 2. `dialog_cancelled` says `restart_save` and `escape`
    }

    @Test
    fun theSaveDialogSaysItWasOpenedFromRestart() {
        // TODO: 1. Choose Save
        // TODO: 2. `screen_save_dialog` says `restart`
    }

    @Test
    fun aDimmedRestartSendsNothing() {
        // TODO: 1. Press R at the start
        // TODO: 2. No event is sent
    }

    @Test
    fun boardSolvedCarriesTheRestartCount() {
        // TODO: 1. Solve without restarting: the count is 0
        // TODO: 2. Restart once, then solve: the count is 1
    }

    @Test
    fun aNewGameStartsTheCountAgain() {
        // TODO: 1. Restart, then start a new game
        // TODO: 2. Solve
        // TODO: 3. `restart_count` is 0
    }

    @Test
    fun aLoadStartsTheCountAgain() {
        // TODO: 1. Restart, then load a save
        // TODO: 2. Solve
        // TODO: 3. `restart_count` is 0
    }

    @Test
    fun shiftSourcesCountOnlySinceTheRestart() {
        // TODO: 1. Shift by drag, restart, then shift by arrow
        // TODO: 2. Solve
        // TODO: 3. `board_solved` counts only the arrow
    }
}
