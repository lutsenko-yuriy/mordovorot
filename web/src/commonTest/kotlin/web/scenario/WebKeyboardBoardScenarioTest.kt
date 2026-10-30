package web.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import viewmodel.KeyboardLayout
import viewmodel.TileSelection
import web.Arrow
import web.Focus
import web.KeyHints
import web.PlayMode
import web.Slides
import web.WebDialog

/** GH-171: the board from the keyboard - the selected tile, its shifts in both layouts, tile presses,
 *  focus and the key hints. The grid's markup is checked by the harness (`keys_check.sh`). */
class WebKeyboardBoardScenarioTest {

    /** No single shift solves it, and every tile differs from the one it would be shifted to. */
    private val mixedUp = intArrayOf(5, 1, 2, 3, 4, 9, 6, 7, 8, 13, 10, 11, 12, 0, 14, 15)

    private fun WebHarness.key(key: String, repeat: Boolean = false) = press(key, from = Focus.Board, repeat = repeat)

    private fun WebHarness.row(row: Int) = state.tiles.subList(row * state.side, (row + 1) * state.side)

    private fun WebHarness.column(col: Int) = (0 until state.side).map { state.tiles[it * state.side + col] }

    /** [keys] each turn the line [moved] reads as [rotated] has it, and leave the selection and the other lines alone. */
    private fun WebHarness.assertShifts(keys: List<String>, moved: () -> List<Int>, rotated: (List<Int>) -> List<Int>) {
        for (key in keys) {
            val before = state
            val line = moved()
            assertTrue(key(key), key)
            assertEquals(rotated(line), moved(), key)
            assertEquals(before.selection, state.selection, key)
            assertEquals(before.tiles.size - line.size, before.tiles.indices.count { before.tiles[it] == state.tiles[it] }, key)
        }
    }

    private fun left(line: List<Int>) = line.drop(1) + line.first()

    private fun right(line: List<Int>) = listOf(line.last()) + line.dropLast(1)

    @Test
    fun `W A S D move the selected tile with wraparound`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        val tiles = page.state.tiles
        assertEquals(TileSelection(0, 0), page.state.selection)

        page.key("d")
        page.key("s")
        assertEquals(TileSelection(1, 1), page.state.selection)
        page.key("a")
        page.key("w")
        assertEquals(TileSelection(0, 0), page.state.selection)
        page.key("w")
        page.key("a")
        assertEquals(TileSelection(3, 3), page.state.selection)
        assertEquals(tiles, page.state.tiles)
    }

    /** So VoiceOver moves on to the new tile at once, rather than hearing a rebuilt board. */
    @Test
    fun `a select key only moves the selection, while a shift or a tile press redraws`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        fun moved(action: () -> Unit): Boolean {
            val before = page.state
            action()
            return page.state.onlyMovesSelectionFrom(before)
        }

        assertTrue(moved { page.key("d") })
        assertFalse(moved { page.key("h") })
        assertTrue(moved { page.key("s") }, "right after a shift's slide too")
        assertFalse(moved { page.game.onTilePressed(3, 3) })
        assertFalse(moved { page.key("2") })
    }

    @Test
    fun `a held select key keeps moving, a held digit doesn't`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.key("d")
        page.key("d", repeat = true)
        assertEquals(TileSelection(0, 2), page.state.selection)

        assertFalse(page.key("2", repeat = true))
        assertNull(page.state.dialog)
    }

    @Test
    fun `H L and the left and right arrows shift the selected row and the selection stays put`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.key("s")

        page.assertShifts(listOf("h", "ArrowLeft"), { page.row(1) }, ::left)
        page.assertShifts(listOf("l", "ArrowRight"), { page.row(1) }, ::right)
    }

    @Test
    fun `K J and the up and down arrows shift the selected column`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.key("d")

        page.assertShifts(listOf("k", "ArrowUp"), { page.column(1) }, ::left)
        page.assertShifts(listOf("j", "ArrowDown"), { page.column(1) }, ::right)
    }

    @Test
    fun `a key shift slides the line and says it as an arrow does`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.key("s")

        page.key("h")

        assertEquals(Slides.ofArrow(Arrow.LEFT, 1), page.state.slide)
        assertEquals(listOf("Row 2 left, 1 move"), page.said)
    }

    @Test
    fun `the left-handed layout selects with HJKL and the arrows and shifts with WASD`() = webTest {
        val page = WebHarness(backgroundScope).remembering(keys = KeyboardLayout.LEFT_HANDED).startOn(mixedUp)
        val tiles = page.state.tiles

        page.key("l")
        page.key("j")
        assertEquals(TileSelection(1, 1), page.state.selection)
        page.key("ArrowRight")
        page.key("ArrowDown")
        assertEquals(TileSelection(2, 2), page.state.selection)
        page.key("h")
        page.key("k")
        page.key("ArrowLeft")
        page.key("ArrowUp")
        assertEquals(TileSelection(0, 0), page.state.selection)
        assertEquals(tiles, page.state.tiles)

        page.assertShifts(listOf("a"), { page.row(0) }, ::left)
        page.assertShifts(listOf("d"), { page.row(0) }, ::right)
        page.assertShifts(listOf("w"), { page.column(0) }, ::left)
        page.assertShifts(listOf("s"), { page.column(0) }, ::right)
    }

    @Test
    fun `upper-case letters work too`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.key("D")
        page.key("S")
        assertEquals(TileSelection(1, 1), page.state.selection)
        page.assertShifts(listOf("H"), { page.row(1) }, ::left)
    }

    @Test
    fun `board keys work only while the board has focus`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        val before = page.state.unfocused()

        assertFalse(page.press("w", from = Focus.NewButton))
        assertFalse(page.press("h", from = Focus.NewButton))
        assertFalse(page.press("h"))
        assertEquals(before, page.state.unfocused())

        assertTrue(page.press("2", from = Focus.NewButton))
        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun `mouse mode has no selection, no board keys and no hints`() = webTest {
        val page = WebHarness(backgroundScope).startOn(mixedUp)

        assertNull(page.state.selection)
        assertNull(page.state.hints)
        assertFalse(page.press("h", from = Focus.Board))
        val frames = page.frames.size
        page.game.onTilePressed(1, 1)
        assertEquals(frames, page.frames.size)
    }

    @Test
    fun `the board takes focus after the startup dialog in keyboard mode, not in mouse mode`() = webTest {
        val keyboard = WebHarness(backgroundScope).remembering().seed("kept", mixedUp).start()
        assertIs<WebDialog.Load>(keyboard.state.dialog)
        keyboard.game.onLoadChosen("kept")
        assertEquals(Focus.Board, keyboard.state.focus)

        val mouse = WebHarness(backgroundScope).seed("kept", mixedUp).start()
        mouse.game.onLoadChosen("kept")
        assertEquals(Focus.PageStart, mouse.state.focus)
    }

    @Test
    fun `entering keyboard mode gives the board focus, and leaving it asks for it where it was`() = webTest {
        val page = WebHarness(backgroundScope).startOn(mixedUp)
        page.game.controls.open()
        page.game.controls.onMode(PlayMode.KEYBOARD)
        page.game.controls.onAnswered(ok = true)
        assertEquals(Focus.Board, page.state.focus)

        page.press("6", from = Focus.Board)
        page.game.controls.onMode(PlayMode.MOUSE)
        page.game.controls.onAnswered(ok = true)
        // Gone in mouse mode, so the page falls back to Controls (PageFocusTest).
        assertEquals(Focus.Board, page.state.focus)
        assertEquals(PlayMode.MOUSE, page.state.mode)
    }

    @Test
    fun `pressing a tile selects it and gives the board focus`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)

        page.game.onTilePressed(2, 1)

        assertEquals(TileSelection(2, 1), page.state.selection)
        assertEquals(Focus.Board, page.state.focus)
    }

    @Test
    fun `a drag leaves the selection on the dragged line and the mouse still works`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        val row = page.row(1)

        page.game.onTilePressed(1, 2)
        page.game.onDragStart(1, 2, 0.0, 0.0, 50.0)
        page.game.onDragMove(-50.0, 0.0)
        page.game.onDragEnd()

        assertEquals(left(row), page.row(1))
        assertEquals(TileSelection(1, 2), page.state.selection)

        page.game.onSave()
        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun `a smaller board clamps the selection and a new game or load keeps it`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.seed("four", mixedUp)
        page.key("w")
        page.key("a")
        val corner = TileSelection(3, 3)
        assertEquals(corner, page.state.selection)

        page.press("1", from = Focus.Board)
        page.game.onSizeChosen(4)
        assertEquals(corner, page.state.selection)

        page.press("3", from = Focus.Board)
        page.game.onLoadChosen("four")
        assertEquals(corner, page.state.selection)

        page.press("1", from = Focus.Board)
        page.game.onSizeChosen(3)
        assertEquals(TileSelection(2, 2), page.state.selection)
    }

    @Test
    fun `switching mode keeps the selection`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn(mixedUp)
        page.game.onTilePressed(1, 2)

        for (mode in listOf(PlayMode.MOUSE, PlayMode.KEYBOARD)) {
            page.game.controls.open()
            page.game.controls.onMode(mode)
            page.game.controls.onAnswered(ok = true)
        }

        assertEquals(TileSelection(1, 2), page.state.selection)
    }

    @Test
    fun `a solved board hides the selection and ignores the select and shift keys`() = webTest {
        val page = WebHarness(backgroundScope).remembering().startOn()
        assertTrue(page.key("h"))
        assertTrue(page.state.solved)
        assertNull(page.state.selection)
        val tiles = page.state.tiles

        assertFalse(page.key("w"))
        assertFalse(page.key("h"))
        assertEquals(tiles, page.state.tiles)

        assertTrue(page.key("2"))
        assertIs<WebDialog.Save>(page.state.dialog)
    }

    @Test
    fun `keyboard mode shows the key hints for its layout, on a solved board too`() = webTest {
        val right = WebHarness(backgroundScope).remembering().startOn()
        assertEquals(KeyHints.board(KeyboardLayout.RIGHT_HANDED), right.state.hints)
        right.key("h")
        assertEquals(KeyHints.board(KeyboardLayout.RIGHT_HANDED), right.state.hints)

        val left = WebHarness(backgroundScope).remembering(keys = KeyboardLayout.LEFT_HANDED).startOn()
        assertEquals(KeyHints.board(KeyboardLayout.LEFT_HANDED), left.state.hints)
        assertNotEquals(right.state.hints, left.state.hints)
    }
}
