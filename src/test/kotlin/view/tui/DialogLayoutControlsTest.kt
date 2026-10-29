package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import viewmodel.KeyboardLayout

/** GH-170: the Controls dialog's geometry and drawing - one row per option group, the key list
 *  under it, the focused group's marked option in reverse video. */
class DialogLayoutControlsTest {

    private val terminalSize = TerminalSize(80, 24)

    private fun dialog(focusedGroup: Int? = null) = Dialog(
        kind = Dialog.Kind.CONTROLS,
        title = "Controls",
        optionGroups = listOf(DialogOptionGroup("Play with", listOf("Mouse", "Keyboard"), 1)),
        notes = BoardKeys.keyList(KeyboardLayout.RIGHT_HANDED),
        buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
        focusedGroup = focusedGroup,
    )

    private fun rows(frame: String) = frame.replace(Regex("\u001B\\[[0-9;?]*[A-Za-z]"), "").split("\r\n")

    private fun render(dialog: Dialog) = ScreenRenderer().render(
        ScreenState.forBoard(board = (0..15).toList(), squareSide = 4, solved = false).copy(dialog = dialog),
        terminalSize,
    )

    @Test
    fun `the group row, a blank row, the key list, a blank row and the buttons, inside 24 rows`() {
        val layout = DialogLayout(dialog(), terminalSize)
        val rows = rows(render(dialog()))

        assertEquals(layout.titleRow() + 2, layout.groupRowPosition(0))
        assertEquals(layout.groupRowPosition(0) + 2, layout.noteRowPosition(0))
        assertEquals(layout.noteRowPosition(BoardKeys.keyList(KeyboardLayout.RIGHT_HANDED).lastIndex) + 2, layout.buttonsRow())
        assertTrue(layout.titleRow() >= 1 && layout.bottomRow() + 1 < 24)
        assertEquals("Play with      ○ Mouse  ● Keyboard", rows[layout.groupRowPosition(0)].substring(layout.left + 2).trimEnd('│', ' '))
        BoardKeys.keyList(KeyboardLayout.RIGHT_HANDED).forEachIndexed { i, line -> assertTrue(line in rows[layout.noteRowPosition(i)], line) }
    }

    @Test
    fun `each option hit-tests to its DialogOption`() {
        val layout = DialogLayout(dialog(), terminalSize)
        val y = layout.groupRowPosition(0)
        val (mouse, keyboard) = layout.options(0)

        assertEquals(HitTarget.DialogOption(0, 0), layout.hitTest(mouse.range.last, y))
        assertEquals(HitTarget.DialogOption(0, 1), layout.hitTest(keyboard.range.first, y))
        assertEquals(HitTarget.Nothing, layout.hitTest(layout.left + 2, y))
        assertEquals(HitTarget.Nothing, layout.hitTest(keyboard.range.first, y + 1))
    }

    @Test
    fun `only the focused group's marked option is reverse video`() {
        assertTrue("\u001B[7m● Keyboard\u001B[27m" in render(dialog(focusedGroup = 0)))
        assertTrue("\u001B[7m" !in render(dialog()))
    }

    @Test
    fun `both groups and the longer left-handed key list fit 80x24, the second group hit-testing as group 1`() {
        val both = dialog().copy(
            optionGroups = dialog().optionGroups + DialogOptionGroup("Keyboard keys", listOf("Right-handed", "Left-handed"), 1),
            notes = BoardKeys.keyList(KeyboardLayout.LEFT_HANDED),
        )
        val layout = DialogLayout(both, terminalSize)
        val rows = rows(render(both))
        val (_, leftHanded) = layout.options(1)

        assertEquals(layout.groupRowPosition(0) + 1, layout.groupRowPosition(1))
        assertTrue(layout.titleRow() >= 1 && layout.bottomRow() + 1 < 24)
        assertEquals("Keyboard keys  ○ Right-handed  ● Left-handed", rows[layout.groupRowPosition(1)].substring(layout.left + 2).trimEnd('│', ' '))
        assertEquals(HitTarget.DialogOption(1, 1), layout.hitTest(leftHanded.range.first, layout.groupRowPosition(1)))
        BoardKeys.keyList(KeyboardLayout.LEFT_HANDED).forEachIndexed { i, line -> assertTrue(line in rows[layout.noteRowPosition(i)], line) }
    }
}
