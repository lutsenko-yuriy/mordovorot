package viewmodel

/**
 * Signals that the current session should end - thrown by [viewmodel.ViewModelImpl.exitGame] after its
 * save-before-quitting dialogue completes. Public (rather than a private nested class of
 * [viewmodel.ViewModelImpl]) so [view.tui.TuiView] (GH-3) can catch it from its own event loop.
 */
class ExitRequestedException : SessionControlException()
