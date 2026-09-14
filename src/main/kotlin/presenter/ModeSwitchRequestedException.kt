package presenter

import InputMode

/** Unwinds `View.play()` to `GameSession.run`, which rebuilds for [target] - same idea as
 *  [ExitRequestedException] but for a mode switch instead of a quit (GH-30). */
class ModeSwitchRequestedException(val target: InputMode) : Exception()
