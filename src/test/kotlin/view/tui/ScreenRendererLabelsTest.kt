package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import viewmodel.TileLabels

/** GH-139's tile labels in [ScreenRenderer]: the grid in each scheme, and the Labels dialog's markers. */
class ScreenRendererLabelsTest {

    private val terminalSize = TerminalSize(columns = 80, rows = 40)
    private val renderer = ScreenRenderer()

    /** The frame's text rows, ANSI escapes stripped. */
    private fun rowsOf(frame: String): List<String> =
        frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    /** The grid's tile texts, row by row. */
    private fun tilesOf(frame: String, side: Int): List<String> {
        val layout = BoardLayout(terminalSize, side)
        val (left, top) = layout.gridOrigin()
        return (0 until side).flatMap { row ->
            rowsOf(frame)[top + 1 + 2 * row].substring(left, left + layout.gridWidth).split("│").filter { it.isNotBlank() }.map { it.trim() }
        }
    }

    @Test
    fun `the grid shows the chosen tile labels (GH-139)`() {
        val board = listOf(6, 0, 15) + (1..5) + (7..14)
        val expected = mapOf(
            TileLabels.NUMBERS to listOf("7", "1", "16"),
            TileLabels.NUMBERS_FROM_0 to listOf("6", "0", "15"),
            TileLabels.PAIRS to listOf("23", "11", "44"),
            TileLabels.PAIRS_FROM_0 to listOf("12", "00", "33"),
            TileLabels.LETTERS to listOf("G", "A", "P"),
            TileLabels.LETTER_PAIRS to listOf("BC", "AA", "DD"),
        )
        for ((labels, firstThree) in expected) {
            val state = ScreenState.forBoard(board, squareSide = 4, solved = false, tileLabels = labels)
            assertEquals(firstThree, tilesOf(renderer.render(state, terminalSize), 4).take(3), "$labels")
        }
    }

    @Test
    fun `a Labels dialog marks its selected row with a filled circle and the rest with hollow ones (GH-139)`() {
        val state = ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(
            dialog = Dialog(
                kind = Dialog.Kind.LABELS,
                title = "Tile labels",
                listItems = listOf("Numbers", "Pairs"),
                selectedIndex = 1,
                buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
            ),
        )

        val text = rowsOf(renderer.render(state, terminalSize)).joinToString("\n")
        assertTrue("○ Numbers" in text && "● Pairs" in text, text)
        assertFalse("> " in text)
    }
}
