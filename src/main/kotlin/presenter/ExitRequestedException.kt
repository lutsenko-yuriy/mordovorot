package presenter

/**
 * Signals that the current session should end - thrown by [presenter.BasePresenter.exitGame] after its
 * save-before-quitting dialogue completes, the same way [view.EndOfInputException] does.
 * Public (rather than a private nested class of [presenter.BasePresenter]) so [view.tui.TuiView]
 * (GH-3) can catch it from its own event loop, since it doesn't call [Presenter.play].
 */
class ExitRequestedException : Exception()
