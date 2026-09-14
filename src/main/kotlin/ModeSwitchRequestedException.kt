/**
 * Signals that the running session should rebuild around a different [InputMode] - thrown by
 * [ModeSwitcherImpl.switchTo] the same way [presenter.ExitRequestedException] signals a session
 * should end. Unwinds whichever `View.play()` is running (`view.ViewImpl`'s console loop,
 * `view.tui.TuiView`'s event loop) back to [GameSession.run], which catches it, rebuilds the
 * View/presenter/analytics stack for [target] around the same board/saves, and re-enters
 * `play()` (GH-30).
 */
class ModeSwitchRequestedException(val target: InputMode) : Exception()
