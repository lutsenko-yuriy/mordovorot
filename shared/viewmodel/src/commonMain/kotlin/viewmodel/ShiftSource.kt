package viewmodel

/** What made a shift (GH-110); `null` for a terminal mouse click. */
sealed interface ShiftSource

/** Which kind of key made a keyboard-mode shift (GH-101). */
enum class ShiftKey : ShiftSource { ARROW, HJKL }

/** A web shift (GH-110): an arrow click or a drag. */
enum class PointerShift : ShiftSource { ARROW, DRAG }

/** A web key shift (GH-171): [key] is `null` for a left-handed one, which `shift_keys` leaves out. */
data class KeyShift(val key: ShiftKey?) : ShiftSource
