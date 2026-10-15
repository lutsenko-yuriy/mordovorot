import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** GH-79 scenarios: console mode removed - the no-terminal abort, the `--console` notice, the TUI
 *  without a console switch, the failed-switch fallback, and old saves still loading. */
class ConsoleRemovalScenarioTest {

    @Test
    fun `a launch with no terminal prints the abort message and starts no game`() {
        val cases = listOf(
            emptyArray<String>() to "none",
            arrayOf("--mouse") to "mouse",
            arrayOf("--keyboard") to "keyboard",
            arrayOf("--console") to "console",
        )
        for ((args, requested) in cases) {
            val analytics = RecordingAnalyticsService()
            val messages = mutableListOf<String>()

            val mode = resolveInputMode(
                args, analytics,
                reportNoInteractiveTerminal = { messages += it },
                hasInteractiveTerminal = { false },
            )

            assertNull(mode, "args=${args.toList()}")
            assertEquals(listOf("No interactive terminal found - Mordovorot only runs as a terminal UI."), messages)
            assertEquals(
                listOf(Event("app_launch_aborted", mapOf("reason" to "no_interactive_terminal", "requested_mode" to requested))),
                analytics.events,
                "args=${args.toList()}",
            )
        }
    }

    @Test
    fun `--keyboard outranks --console in requested_mode`() {
        val analytics = RecordingAnalyticsService()

        resolveInputMode(arrayOf("--console", "--keyboard"), analytics, reportNoInteractiveTerminal = {}, hasInteractiveTerminal = { false })

        assertEquals("keyboard", analytics.events.single().properties["requested_mode"])
    }

    @Test
    fun `--console in a terminal shows the removal notice and starts the mouse TUI`() {
        val analytics = RecordingAnalyticsService()
        val notices = mutableListOf<String>()
        val unrecognized = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--console"), analytics,
            warnUnrecognizedArg = { unrecognized += it },
            noticeConsoleRemoved = { notices += it },
            hasInteractiveTerminal = { true },
        )

        assertEquals(InputMode.MOUSE, mode)
        assertEquals(listOf("Console mode was removed - starting the terminal UI."), notices)
        assertEquals(
            listOf(Event("app_launched", mapOf("mode" to "mouse", "board_size" to 4, "console_flag_ignored" to true))),
            analytics.events,
        )
        assertEquals(emptyList(), unrecognized)
    }

    @Test
    fun `--console with --keyboard starts keyboard mode with the notice`() {
        val notices = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--console", "--keyboard"), RecordingAnalyticsService(),
            warnUnrecognizedArg = { warnings += it },
            noticeConsoleRemoved = { notices += it },
            hasInteractiveTerminal = { true },
        )

        assertEquals(InputMode.KEYBOARD, mode)
        assertEquals(1, notices.size)
        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `a normal launch reports console_flag_ignored=false and no notice`() {
        val analytics = RecordingAnalyticsService()
        val notices = mutableListOf<String>()

        resolveInputMode(emptyArray(), analytics, noticeConsoleRemoved = { notices += it }, hasInteractiveTerminal = { true })

        assertEquals(false, analytics.events.single().properties["console_flag_ignored"])
        assertEquals(emptyList(), notices)
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
