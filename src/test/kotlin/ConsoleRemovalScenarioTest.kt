import kotlin.test.Test

/** GH-79 scenarios: console mode removed - the no-terminal abort, the `--console` notice, the TUI
 *  without a console switch, the failed-switch fallback, and old saves still loading. */
class ConsoleRemovalScenarioTest {

    @Test
    fun `a launch with no terminal prints the abort message and starts no game`() {
        // TODO: for each of no flag, --mouse, --keyboard, --console: resolve the launch with no
        //  interactive terminal
        // TODO: the result is "don't start" (null)
        // TODO: the message sink got "No interactive terminal found - Mordovorot only runs as a terminal UI."
        // TODO: app_launch_aborted tracked with reason=no_interactive_terminal and requested_mode
        //  none / mouse / keyboard / console respectively
        // TODO: no app_launched tracked
    }

    @Test
    fun `--keyboard outranks --console in requested_mode`() {
        // TODO: resolve --console --keyboard with no interactive terminal
        // TODO: app_launch_aborted has requested_mode=keyboard
    }

    @Test
    fun `--console in a terminal shows the removal notice and starts the mouse TUI`() {
        // TODO: resolve --console with an interactive terminal
        // TODO: resolves to MOUSE
        // TODO: the notice sink got "Console mode was removed - starting the terminal UI."
        // TODO: app_launched has mode=mouse and console_flag_ignored=true
        // TODO: no unrecognized-argument warning
    }

    @Test
    fun `--console with --keyboard starts keyboard mode with the notice`() {
        // TODO: resolve --console --keyboard with an interactive terminal
        // TODO: resolves to KEYBOARD
        // TODO: the removal notice is shown
        // TODO: no "--keyboard ignored" warning
    }

    @Test
    fun `a normal launch reports console_flag_ignored=false and no notice`() {
        // TODO: resolve no flags with an interactive terminal
        // TODO: app_launched has console_flag_ignored=false
        // TODO: the notice sink was not called
    }

    @Test
    fun `the mouse toolbar offers only the Keyboard switch`() {
        // TODO: run a mouse-mode session with no events
        // TODO: the first frame contains "[ Keyboard ]" and no "Console"
    }

    @Test
    fun `keyboard mode offers only Mouse F7, and F8 does nothing`() {
        // TODO: run a keyboard-mode session with events F8, then end of input
        // TODO: frames contain "[Mouse F7]", no "Console", and the hint line has no "F8"
        // TODO: no mode switch was requested and the board is unchanged
    }

    @Test
    fun `a failed switch falls back to the previous TUI mode and keeps the game`() {
        // TODO: mouse session; shift row 1; click [ Keyboard ]
        // TODO: the keyboard rebuild throws
        // TODO: "Could not switch to keyboard mode (...) - staying in mouse mode." is shown
        // TODO: mouse mode is rebuilt with the same board and Moves: 1, with no restore prompt
    }

    @Test
    fun `a save made before GH-79 still restores at TUI startup`() {
        // TODO: seed old.save (4x4 format with a move count)
        // TODO: mouse session; confirm the restore dialog
        // TODO: the board and Moves: match the save
    }
}
