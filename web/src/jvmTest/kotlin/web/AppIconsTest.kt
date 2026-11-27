package web

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** GH-357: the committed app icons are the board in miniature, in the palette's colours. They are
 *  rendered by `scripts/web/icons/render_icons.js` (which holds the same `grid` fractions as
 *  [Icon]); a palette change that was not followed by a re-render fails here. */
class AppIconsTest {
    private class Icon(val file: String, val size: Int, val grid: Double, val opaque: Boolean, val maskable: Boolean = false)

    private val icons = listOf(
        Icon("icon-192.png", 192, 0.9, opaque = false),
        Icon("icon-512.png", 512, 0.9, opaque = false),
        Icon("favicon-32.png", 32, 0.9, opaque = false),
        Icon("apple-touch-icon.png", 180, 0.72, opaque = true),
        Icon("icon-maskable-512.png", 512, 0.56, opaque = true, maskable = true),
    )

    private val palette = File("src/jsMain/resources/palette.css").readText().substringBefore("@media")
    private fun token(name: String) = Regex("""--$name:\s*([^;]+);""").find(palette)!!.groupValues[1].trim()
    private val frame = Wcag.hex(token("frame"))
    private val tileLight = token("tile-light").removeSuffix("%").toDouble()
    private val tileRamp = token("tile-ramp").removeSuffix("%").toDouble()

    private fun read(icon: Icon): BufferedImage = ImageIO.read(File("src/jsMain/resources/icons/${icon.file}"))
    private fun hint(icon: Icon) = "${icon.file} is stale: run `node scripts/web/icons/render_icons.js`"

    private fun channels(argb: Int) = listOf(argb shr 16 and 0xff, argb shr 8 and 0xff, argb and 0xff)
    private fun channels(c: Wcag.Rgb) = listOf(c.r, c.g, c.b).map { (it * 255).roundToInt() }
    private fun close(a: List<Int>, b: List<Int>) = a.zip(b).all { (x, y) -> abs(x - y) <= 2 }

    private fun assertColour(expected: Wcag.Rgb, img: BufferedImage, x: Int, y: Int, what: String, hint: String) {
        val actual = channels(img.getRGB(x, y))
        assertTrue(close(actual, channels(expected)), "$what at ($x, $y) is $actual, wanted ${channels(expected)}. $hint")
    }

    @Test
    fun `each icon has its declared size`() {
        icons.forEach { icon ->
            val img = read(icon)
            assertEquals(icon.size to icon.size, img.width to img.height, icon.file)
        }
    }

    @Test
    fun `apple touch and maskable icons are opaque, as the platform masks them itself`() {
        icons.filter { it.opaque }.forEach { icon ->
            val img = read(icon)
            for (y in 0 until img.height) for (x in 0 until img.width) {
                assertEquals(0xff, img.getRGB(x, y) ushr 24, "${icon.file} has a transparent pixel at ($x, $y)")
            }
        }
    }

    @Test
    fun `the rounded icons have transparent corners`() {
        icons.filter { !it.opaque }.forEach { icon ->
            assertEquals(0, read(icon).getRGB(0, 0) ushr 24, "${icon.file} corner")
        }
    }

    /** The unshifted rows' tiles take the board's colours: hue row * 120 + 8, lightness along the row. */
    @Test
    fun `tiles of the top and bottom rows have the board's colours`() {
        icons.forEach { icon ->
            val img = read(icon)
            val off = (icon.size - icon.size * icon.grid) / 2
            val pitch = icon.size * icon.grid / 3
            for (row in listOf(0, 2)) for (col in 0..2) {
                val expected = Wcag.hsl(row * 120.0 + 8, 68.0, tileLight - col * tileRamp / 2)
                val x = (off + (col + 0.5) * pitch).toInt()
                val y = (off + (row + 0.5) * pitch).toInt()
                assertColour(expected, img, x, y, "${icon.file} tile row $row col $col", hint(icon))
            }
        }
    }

    /** The middle row is shifted half a tile right and wraps: its last tile is half back in on the left. */
    @Test
    fun `the middle row is shifted half a tile and wraps`() {
        icons.forEach { icon ->
            val img = read(icon)
            val off = (icon.size - icon.size * icon.grid) / 2
            val pitch = icon.size * icon.grid / 3
            val y = (off + 1.5 * pitch).toInt()
            fun row1(col: Int) = Wcag.hsl(128.0, 68.0, tileLight - col * tileRamp / 2)
            assertColour(row1(0), img, (off + 1.0 * pitch).toInt(), y, "${icon.file} middle row, first tile", hint(icon))
            assertColour(row1(1), img, (off + 2.0 * pitch).toInt(), y, "${icon.file} middle row, second tile", hint(icon))
            assertColour(row1(2), img, (off + 0.2 * pitch).toInt(), y, "${icon.file} middle row, wrapped tile", hint(icon))
        }
    }

    @Test
    fun `the frame colour shows between the tiles and around the board`() {
        icons.forEach { icon ->
            val img = read(icon)
            val off = (icon.size - icon.size * icon.grid) / 2
            val pitch = icon.size * icon.grid / 3
            // The gap between the top row's first two tiles (under a pixel wide on the 32 px favicon, so not sampled
            // there), and the margin to the left of the board's middle.
            if (icon.size >= 180) assertColour(frame, img, (off + pitch).toInt(), (off + 0.5 * pitch).toInt(), "${icon.file} gap", hint(icon))
            assertColour(frame, img, (off / 2).toInt(), (icon.size / 2), "${icon.file} margin", hint(icon))
        }
    }

    /** Android crops a maskable icon to a shape that can be as small as the circle of 80% of its width. */
    @Test
    fun `everything but the frame colour on the maskable icon is inside the safe circle`() {
        val icon = icons.first { it.maskable }
        val img = read(icon)
        val centre = icon.size / 2.0
        val radius = icon.size * 0.4
        for (y in 0 until img.height) for (x in 0 until img.width) {
            val isFrame = close(channels(img.getRGB(x, y)), channels(frame))
            if (!isFrame) assertTrue(hypot(x + 0.5 - centre, y + 0.5 - centre) <= radius, "pixel ($x, $y) is outside the safe circle. ${hint(icon)}")
        }
    }
}
