package presenter

/** The console-only presenter surface - [view.ViewImpl] depends on this, not [Presenter]
 *  directly, so it can never reach [TuiPresenter]'s TUI-only query methods (GH-23). */
interface ConsolePresenter : Presenter {

    fun play()
}
