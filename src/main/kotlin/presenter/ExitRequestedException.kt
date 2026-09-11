package presenter

/**
 * Signals that the current session should end - thrown by [PresenterImpl.exitGame] after its
 * save-before-quitting dialogue completes, the same way [view.EndOfInputException] does.
 * Public (rather than a private nested class of [PresenterImpl]) so [view.tui.TuiView]
 * (GH-3) can catch it from its own event loop, since it doesn't call [Presenter.play].
 */
class ExitRequestedException : Exception()
