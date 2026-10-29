package view.tui

import kotlin.test.Test
import kotlin.test.assertEquals

/** GH-170: [DialogKeys] on the Controls dialog - focus starts on the option group, the arrows
 *  move its mark there and focus on the buttons, Enter is OK except on Cancel. In both modes. */
class DialogKeysControlsTest {

    private val terminalSize = TerminalSize(80, 24)

    private fun dialog(selected: Int = 0) = Dialog(
        kind = Dialog.Kind.CONTROLS,
        title = "Controls",
        optionGroups = listOf(DialogOptionGroup("Play with", listOf("Mouse", "Keyboard"), selected)),
        notes = BoardKeys.KEY_LIST,
        buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
    )

    private val inputs = listOf(KeyboardInput(KeyboardSelection()), MouseInput())

    private fun TuiInput.press(event: TerminalEvent, dialog: Dialog = dialog()) =
        onDialogEvent(event, dialog, DialogLayout(dialog, terminalSize))

    @Test
    fun `focus opens on the group, the arrows move its mark with wraparound and Enter submits`() {
        for (input in inputs) {
            input.onDialogOpened()
            assertEquals(0, input.decorateDialog(dialog()).focusedGroup, "$input")
            assertEquals(InputAction.Activate(HitTarget.DialogOption(0, 1)), input.press(TerminalEvent.Arrow(Direction.RIGHT)))
            assertEquals(InputAction.Activate(HitTarget.DialogOption(0, 1)), input.press(TerminalEvent.Arrow(Direction.LEFT)))
            assertEquals(InputAction.Activate(HitTarget.DialogOption(0, 0)), input.press(TerminalEvent.Arrow(Direction.DOWN), dialog(1)))
            assertEquals(InputAction.Activate(HitTarget.DialogOption(0, 0)), input.press(TerminalEvent.Arrow(Direction.UP), dialog(1)))
            assertEquals(InputAction.Submit, input.press(TerminalEvent.Enter))
            assertEquals(InputAction.None, input.press(TerminalEvent.KeyPress('x')))
        }
    }

    @Test
    fun `Tab walks the group, OK and Cancel, where the arrows move focus and Enter activates`() {
        for (input in inputs) {
            input.onDialogOpened()
            input.press(TerminalEvent.Tab)
            assertEquals("ok", input.decorateDialog(dialog()).focusedButtonId, "$input")
            assertEquals(null, input.decorateDialog(dialog()).focusedGroup)
            assertEquals(InputAction.Activate(HitTarget.DialogButton("ok")), input.press(TerminalEvent.Enter))
            input.press(TerminalEvent.Arrow(Direction.RIGHT))
            assertEquals(InputAction.Activate(HitTarget.DialogButton("cancel")), input.press(TerminalEvent.Enter))
            input.press(TerminalEvent.Tab)
            assertEquals(0, input.decorateDialog(dialog()).focusedGroup)
            input.press(TerminalEvent.BackTab)
            assertEquals("cancel", input.decorateDialog(dialog()).focusedButtonId)
            assertEquals(InputAction.Cancel, input.press(TerminalEvent.Escape))
        }
    }

    @Test
    fun `mouse mode clicks an option, and other dialogs keep their plain keys`() {
        val input = MouseInput()
        val layout = DialogLayout(dialog(), terminalSize)
        val keyboard = layout.options(0)[1]

        assertEquals(
            InputAction.Activate(HitTarget.DialogOption(0, 1)),
            input.onDialogEvent(TerminalEvent.MouseClick(keyboard.x, layout.groupRowPosition(0)), dialog(), layout),
        )
        val exit = Dialog(Dialog.Kind.EXIT, "Exit", buttons = listOf(DialogButtonSpec("yes", "Yes")))
        assertEquals(InputAction.None, input.press(TerminalEvent.Tab, exit))
        assertEquals(exit, input.decorateDialog(exit))
    }
}
