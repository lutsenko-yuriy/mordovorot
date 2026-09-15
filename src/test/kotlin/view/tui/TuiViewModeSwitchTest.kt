package view.tui

import InputMode
import presenter.ModeSwitchRequestedException
import presenter.ModeSwitcher
import testing.FakeTerminal
import testing.FakeTuiPresenter
import testing.RecordingModeSwitcher
import testing.runTestBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers GH-30's TUI-side mode-switch triggers: the mouse toolbar's Keyboard/Console buttons
 * and the keyboard mode's F7/F8 shortcuts, both reaching an injected `ModeSwitcher` with the
 * right target mode and trigger, plus the exception unwinding cleanly out of `TuiView.play()`.
 */
class TuiViewModeSwitchTest {

    private val terminalSize = TerminalSize(80, 24)

    /** Same [BoardLayout] MouseInput's decorated `ScreenState` produces on its first repaint -
     *  used to compute click coordinates the same way the production code lays them out. */
    private fun mouseBoardLayout() = BoardLayout(
        terminalSize,
        squareSide = 4,
        modeButtons = listOf(InputMode.KEYBOARD, InputMode.CONSOLE),
    )

    @Test
    fun `clicking the toolbar's Keyboard button requests a switch to keyboard mode with trigger=toolbar`() = runTestBlocking {
        val switcher = RecordingModeSwitcher()
        val button = mouseBoardLayout().toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.KEYBOARD) }
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(button.range.first, button.y)))
        val view = TuiView.create(terminal, modeSwitcherFactory = { switcher }) { FakeTuiPresenter() }

        view.play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.KEYBOARD, "toolbar")), switcher.calls)
    }

    @Test
    fun `clicking the toolbar's Console button requests a switch to console mode with trigger=toolbar`() = runTestBlocking {
        val switcher = RecordingModeSwitcher()
        val button = mouseBoardLayout().toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.CONSOLE) }
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.MouseClick(button.range.first, button.y)))
        val view = TuiView.create(terminal, modeSwitcherFactory = { switcher }) { FakeTuiPresenter() }

        view.play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.CONSOLE, "toolbar")), switcher.calls)
    }

    @Test
    fun `pressing F7 in keyboard mode requests a switch to mouse mode with trigger=shortcut, even when solved`() = runTestBlocking {
        val switcher = RecordingModeSwitcher()
        val presenter = FakeTuiPresenter().apply { solved = true }
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.FunctionKey(7)))
        val view = TuiView.create(terminal, input = KeyboardInput(), modeSwitcherFactory = { switcher }) { presenter }

        view.play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.MOUSE, "shortcut")), switcher.calls)
    }

    @Test
    fun `pressing F8 in keyboard mode requests a switch to console mode with trigger=shortcut`() = runTestBlocking {
        val switcher = RecordingModeSwitcher()
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.FunctionKey(8)))
        val view = TuiView.create(terminal, input = KeyboardInput(), modeSwitcherFactory = { switcher }) { FakeTuiPresenter() }

        view.play()

        assertEquals(listOf(RecordingModeSwitcher.Call(InputMode.CONSOLE, "shortcut")), switcher.calls)
    }

    @Test
    fun `pressing F8 with a real ModeSwitcher unwinds ModeSwitchRequestedException out of play() and restores the terminal`() = runTestBlocking {
        // Audit suggestion on PR #40: the mouse-click path has this coverage (below); the
        // keyboard-shortcut path only had RecordingModeSwitcher tests, leaving the actual
        // unwind-and-restore guarantee untested for F7/F8's route to handleTarget.
        val throwingSwitcher = object : ModeSwitcher {
            override suspend fun switchTo(target: InputMode, trigger: String) = throw ModeSwitchRequestedException(target)
        }
        val terminal = FakeTerminal(events = mutableListOf(TerminalEvent.FunctionKey(8)))
        val view = TuiView.create(terminal, input = KeyboardInput(), modeSwitcherFactory = { throwingSwitcher }) { FakeTuiPresenter() }

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
        assertTrue(terminal.restored)
    }

    @Test
    fun `a mode switch reached after a dialog was opened and cancelled still unwinds cleanly and restores the terminal`() = runTestBlocking {
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

        val boardLayout = mouseBoardLayout()
        val saveButton = boardLayout.toolbarButtons().first { it.target == HitTarget.ToolbarSave }
        val consoleButton = boardLayout.toolbarButtons().first { it.target == HitTarget.ToolbarMode(InputMode.CONSOLE) }

        val throwingSwitcher = object : ModeSwitcher {
            override suspend fun switchTo(target: InputMode, trigger: String) = throw ModeSwitchRequestedException(target)
        }
        val terminal = FakeTerminal(
            events = mutableListOf(
                TerminalEvent.MouseClick(saveButton.range.first, saveButton.y), // opens the Save dialog
                TerminalEvent.MouseClick(cancelButton.range.first, dialogLayout.buttonsRow()), // cancels it
                TerminalEvent.MouseClick(consoleButton.range.first, consoleButton.y), // requests the switch
            ),
            terminalSize = terminalSize,
        )
        val view = TuiView.create(terminal, modeSwitcherFactory = { throwingSwitcher }) { FakeTuiPresenter() }

        assertFailsWith<ModeSwitchRequestedException> { view.play() }
        assertTrue(terminal.restored)
    }
}
