package viewmodel

/** The terminal's viewModel (GH-41): [GameViewModel] plus Exit, which only the terminal offers.
 *  What `view.tui.TuiView` depends on. */
interface ViewModel : GameViewModel {

    /** Ends the current session (toolbar Exit) after an optional save-first dialogue. Usually
     *  ends the owning View's play loop outright; returns normally instead, leaving the session
     *  running, if the user asked to save but the save attempt failed - see
     *  [viewmodel.usecase.ExitGameUseCase.exitGame]. */
    suspend fun exitGame()
}
