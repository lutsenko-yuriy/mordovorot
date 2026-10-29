package web

/** The browser's default font size and iOS's Dynamic Type body size at the standard settings. */
private const val STANDARD_ROOT_PX = 16.0
private const val STANDARD_APPLE_BODY_PX = 17.0

/** GH-168: how much larger than standard the player's system text size is, from the browser's default
 *  font size ([rootPx]) and, on iOS, `-apple-system-body`'s ([appleBodyPx]). Never under 1, so text
 *  never shrinks; a missing or broken reading counts as standard. */
fun textScaleOf(rootPx: Double?, appleBodyPx: Double?): Double =
    maxOf(1.0, ratio(rootPx, STANDARD_ROOT_PX), ratio(appleBodyPx, STANDARD_APPLE_BODY_PX))

/** The root's font size for [scale], which every rem follows. */
fun rootFontPx(scale: Double): Double = STANDARD_ROOT_PX * scale

private fun ratio(px: Double?, standard: Double): Double =
    if (px != null && px.isFinite() && px > 0) px / standard else 1.0
