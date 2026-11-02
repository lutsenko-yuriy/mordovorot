package web.qr

/** A [QrCode] as SVG numbers, DOM-free so tests can check them; the page builds the element (GH-243). */
object QrSvg {
    /** The white border around the code, in modules (the standard's minimum). */
    const val QUIET_ZONE = 4

    /** The width and height of the drawing, in modules, border included. */
    fun extent(code: QrCode): Int = code.size + 2 * QUIET_ZONE

    fun viewBox(code: QrCode): String = "0 0 ${extent(code)} ${extent(code)}"

    /** One path of the dark modules, a rectangle per horizontal run, in the viewBox's coordinates. */
    fun path(code: QrCode): String = buildString {
        for (y in 0 until code.size) {
            var x = 0
            while (x < code.size) {
                if (!code.isDark(x, y)) {
                    x++
                    continue
                }
                val start = x
                while (code.isDark(x, y)) x++
                append("M${start + QUIET_ZONE} ${y + QUIET_ZONE}h${x - start}v1h-${x - start}z")
            }
        }
    }
}
