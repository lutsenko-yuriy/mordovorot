package web.a11y

/** A label changing as part of a label wave (GH-342): the [old] one fades out [leaveMs] from now and the new one fades in [arriveMs] from now
 *  (negative once under way), as CSS `animation-delay`s. [id] tells one wave from the next. */
internal class Waving(val old: String, val leaveMs: Double, val arriveMs: Double, val id: Int)
