package presenter

/**
 * Signals that the current session should end - thrown by [presenter.PresenterImpl.exitGame] after its
 * save-before-quitting dialogue completes, the same way [view.EndOfInputException] does.
 * Public (rather than a private nested class of [presenter.PresenterImpl]) so [view.tui.TuiView]
 * (GH-3) can catch it from its own event loop, since it doesn't call [view.ViewImpl.play]'s
 * console-only loop.
 */
class ExitRequestedException : SessionControlException()
