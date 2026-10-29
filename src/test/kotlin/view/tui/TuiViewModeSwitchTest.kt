package view.tui

import InputMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import testing.FakeTerminal
import testing.FakeViewModel
import testing.RecordingAnalyticsService
import testing.RecordingModeSwitcher
import testing.testTuiView
import viewmodel.ModeSwitchRequestedException
import viewmodel.ModeSwitcher

/**
 * The TUI's mode switch (GH-30), through the Controls dialog since GH-170: `[ Controls 6 ]` or 6
 * opens it, OK with the other mode marked reaches the injected `ModeSwitcher` with trigger
 * `controls_dialog`, and the exception unwinds cleanly out of `TuiView.play()`.
 */
class TuiViewModeSwitchTest {

    private val terminalSize = TerminalSize(80, 24)

    private fun mouseBoardLayout() = BoardLayout(terminalSize, squareSide = 4)

    /** The Controls dialog's geometry with [marked] marked, as the controller builds it. */
    private fun controlsLayout(marked: InputMode) = DialogLayout(
        Dialog(
            kind = Dialog.Kind.CONTROLS,
            title = "Controls",
            optionGroups = listOf(DialogOptionGroup("Play with", listOf("Mouse", "Keyboard"), marked.ordinal)),
            notes = BoardKeys.KEY_LIST,
            buttons = listOf(DialogButtonSpec("ok", "OK"), DialogButtonSpec("cancel", "Cancel")),
        ),
        terminalSize,
    )

    private fun click(button: DialogButtonLayout, y: Int) = TerminalEvent.MouseClick(button.x, y)

    /** Clicks `[ Controls 6 ]`, then Keyboard, then OK. */
    private fun clickToKeyboard(): List<TerminalEvent> {
        val (controlsX, controlsY) = mouseBoardLayout().controlsButtonPosition()
        val dialog = controlsLayout(InputMode.MOUSE)
        val keyboardOption = dialog.options(0)[1]
        val ok = dialog.buttons().first { it.target == HitTarget.DialogButton("ok") }
        return listOf(
            TerminalEvent.MouseClick(controlsX, controlsY),
            click(keyboardOption, dialog.groupRowPosition(0)),
            click(ok, controlsLayout(InputMode.KEYBOARD).buttonsRow()),
        )
    }

    private val throwingSwitcher = object : ModeSwitcher {
        override val currentMode = InputMode.KEYBOARD
        override suspend fun switchTo(target: InputMode, trigger: String) = throw ModeSwitchRequestedException(target)
    }

    @Test
    fun `clicking Controls, Keyboard and OK requests keyboard mode with trigger controls_dialog`(): Unit = runBlocking {
        val switcher = RecordingModeSwitcher()
        val viewModel = FakeViewModel()
        val terminal = FakeTerminal(events = clickToKeyboard().toMutableList(), terminalSize = terminalSize)

        testTuiView(terminal, modeSwitcher = switcher, viewModel = viewModel).play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.KEYBOARD, "controls_dialog")), switcher.calls)
        assertEquals(listOf("controlsDialogOpened(toolbar)"), viewModel.calls.filter { "controls" in it })
    }

    @Test
    fun `6, the left arrow and Enter in keyboard mode request mouse mode, even when solved`(): Unit = runBlocking {
        val switcher = RecordingModeSwitcher(InputMode.KEYBOARD)
        val viewModel = FakeViewModel().apply { solved = true }
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.KeyPress('6'), TerminalEvent.Arrow(Direction.LEFT), TerminalEvent.Enter))

        testTuiView(terminal, input = KeyboardInput(KeyboardSelection()), modeSwitcher = switcher, viewModel = viewModel).play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.MOUSE, "controls_dialog")), switcher.calls)
        assertEquals(listOf("controlsDialogOpened(shortcut)"), viewModel.calls.filter { "controls" in it })
    }

    @Test
    fun `OK with the current mode marked, Escape and Cancel request no switch`(): Unit = runBlocking {
        val switcher = RecordingModeSwitcher()
        val analytics = RecordingAnalyticsService()
        val dialog = controlsLayout(InputMode.MOUSE)
        val cancel = dialog.buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val events = listOf(TerminalEvent.KeyPress('6'), TerminalEvent.Enter, TerminalEvent.KeyPress('6'), TerminalEvent.Escape) +
            TerminalEvent.KeyPress('6') + click(cancel, dialog.buttonsRow())
        val terminal = FakeTerminal(events = events.toMutableList(), terminalSize = terminalSize)

        testTuiView(terminal, analytics = analytics, modeSwitcher = switcher, viewModel = FakeViewModel()).play()

        assertEquals(emptyList(), switcher.calls)
        assertEquals(listOf("controls", "controls"), analytics.events.filter { it.name == "dialog_cancelled" }.map { it.properties["dialog"] })
    }

    @Test
    fun `5 in keyboard mode requests no switch`(): Unit = runBlocking {
        val switcher = RecordingModeSwitcher(InputMode.KEYBOARD)
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.KeyPress('5')))
        val view = testTuiView(terminal, input = KeyboardInput(KeyboardSelection()), modeSwitcher = switcher, viewModel = FakeViewModel())

        view.play()

        assertEquals(emptyList(), switcher.calls)
    }

    @Test
    fun `a switch with a real ModeSwitcher unwinds ModeSwitchRequestedException out of play() and restores the terminal`(): Unit = runBlocking {
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.KeyPress('6'), TerminalEvent.Arrow(Direction.LEFT), TerminalEvent.Enter))
        val view = testTuiView(terminal, input = KeyboardInput(KeyboardSelection()), modeSwitcher = throwingSwitcher, viewModel = FakeViewModel())

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
        assertTrue(terminal.restored)
    }

    @Test
    fun `a mode switch reached after a dialog was opened and cancelled still unwinds cleanly and restores the terminal`(): Unit = runBlocking {
        // A Save dialog exactly as handleToolbarSave's runSaveDialog builds it on its first
        // iteration (empty typed name, no overwrite warning) - needed to compute the Cancel
        // button's real coordinates.
        val saveDialog = Dialog(
            kind = Dialog.Kind.SAVE,
            title = "Save game",
            textFieldValue = "",
            buttons = listOf(DialogButtonSpec("save", "Save"), DialogButtonSpec("cancel", "Cancel")),
        )
        val dialogLayout = DialogLayout(saveDialog, terminalSize)
        val cancelButton = dialogLayout.buttons().first { it.target == HitTarget.DialogButton("cancel") }
        val saveButton = mouseBoardLayout().toolbarButtons().first { it.target == HitTarget.ToolbarSave }
        val mouseSwitcher = object : ModeSwitcher by throwingSwitcher {
            override val currentMode = InputMode.MOUSE
        }
        val terminal = FakeTerminal(
            events = (
                listOf(
                    TerminalEvent.MouseClick(saveButton.range.first, saveButton.y), // opens the Save dialog
                    TerminalEvent.MouseClick(cancelButton.range.first, dialogLayout.buttonsRow()), // cancels it
                ) + clickToKeyboard()
                ).toMutableList(),
            terminalSize = terminalSize,
        )
        val view = testTuiView(terminal, modeSwitcher = mouseSwitcher, viewModel = FakeViewModel())

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
        assertTrue(terminal.restored)
    }
}
