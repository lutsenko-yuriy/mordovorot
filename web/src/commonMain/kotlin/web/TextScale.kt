package web

import kotlin.math.abs

/** The browser's default font size and iOS's Dynamic Type body size at the standard settings. */
private const val STANDARD_ROOT_PX = 16.0
private const val STANDARD_APPLE_BODY_PX = 17.0

/** GH-168: how much larger than standard the player's system text size is, from the browser's default
 *  font size ([rootPx]) and, on iOS, `-apple-system-body`'s ([appleBodyPx]). Never under 1, so text
 *  never shrinks; a missing or broken reading counts as standard. */
fun textScaleOf(rootPx: Double?, appleBodyPx: Double?): Double =
    maxOf(1.0, ratio(rootPx, STANDARD_ROOT_PX), ratio(appleBodyPx, STANDARD_APPLE_BODY_PX))

/** The root's font size for [scale], which every rem follows, or null when the browser's default
 *  ([rootPx]) is already that size: set anyway, Firefox's text zoom, if the default carries it, would
 *  apply to it a second time. */
fun rootFontPx(rootPx: Double?, scale: Double): Double? =
    (STANDARD_ROOT_PX * scale).takeUnless { rootPx != null && abs(rootPx - it) < 0.01 }

private fun ratio(px: Double?, standard: Double): Double =
    if (px != null && px.isFinite() && px > 0) px / standard else 1.0
