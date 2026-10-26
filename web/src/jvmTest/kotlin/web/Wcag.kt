package web

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** GH-165: WCAG 2.x contrast for the sheets' colours: `#rgb`/`#rrggbb` hex and `hsl()`. */
object Wcag {
    class Rgb(val r: Double, val g: Double, val b: Double)

    fun hex(value: String): Rgb {
        val digits = value.removePrefix("#").let { if (it.length == 3) it.map { c -> "$c$c" }.joinToString("") else it }
        require(digits.length == 6) { "not a hex colour: $value" }
        val (r, g, b) = (0..2).map { digits.substring(it * 2, it * 2 + 2).toInt(16) / 255.0 }
        return Rgb(r, g, b)
    }

    /** [hue] in degrees, [saturation] and [lightness] in percent, as CSS `hsl()` takes them. */
    fun hsl(hue: Double, saturation: Double, lightness: Double): Rgb {
        val s = saturation / 100
        val l = lightness / 100
        val chroma = (1 - abs(2 * l - 1)) * s
        val h = (hue % 360 + 360) % 360 / 60
        val x = chroma * (1 - abs(h % 2 - 1))
        val (r, g, b) = when (h.toInt()) {
            0 -> Triple(chroma, x, 0.0)
            1 -> Triple(x, chroma, 0.0)
            2 -> Triple(0.0, chroma, x)
            3 -> Triple(0.0, x, chroma)
            4 -> Triple(x, 0.0, chroma)
            else -> Triple(chroma, 0.0, x)
        }
        val m = l - chroma / 2
        return Rgb(r + m, g + m, b + m)
    }

    fun luminance(c: Rgb): Double {
        fun channel(v: Double) = if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
        return 0.2126 * channel(c.r) + 0.7152 * channel(c.g) + 0.0722 * channel(c.b)
    }

    fun contrast(a: Rgb, b: Rgb): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }
}
