import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [InputMode] flag parsing (default mouse TUI, `--keyboard`) and [resolveInputMode]'s
 * `app_launched` tracking and argument warnings. The no-terminal abort and the `--console`
 * notice (GH-79) are covered in `ConsoleRemovalScenarioTest`.
 */
class InputModeTest {

    private val terminal = { true }

    @Test
    fun `no arguments resolves to MOUSE`() {
        assertEquals(InputMode.MOUSE, InputMode.resolve(emptyArray()))
    }

    @Test
    fun `--keyboard resolves to KEYBOARD`() {
        assertEquals(InputMode.KEYBOARD, InputMode.resolve(arrayOf("--keyboard")))
    }

    @Test
    fun `--mouse resolves to MOUSE, same as no flag at all`() {
        assertEquals(InputMode.MOUSE, InputMode.resolve(arrayOf("--mouse")))
    }

    @Test
    fun `--console no longer selects anything - it resolves like no flag`() {
        assertEquals(InputMode.MOUSE, InputMode.resolve(arrayOf("--console")))
    }

    @Test
    fun `app_launched is tracked with the resolved mode`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveInputMode(arrayOf("--keyboard"), analytics, hasInteractiveTerminal = terminal)

        assertEquals(InputMode.KEYBOARD, mode)
        assertEquals(
            listOf(
                Event(
                    "app_launched",
                    mapOf("mode" to "keyboard", "board_size" to 4, "console_flag_ignored" to false, "tile_labels" to "numbers"),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `app_launched carries the remembered tile labels, GH-139`() {
        val analytics = RecordingAnalyticsService()

        resolveInputMode(emptyArray(), analytics, tileLabels = "pairs", hasInteractiveTerminal = terminal)

        assertEquals("pairs", analytics.events.single().properties["tile_labels"])
    }

    @Test
    fun `app_launched carries the resolved board_size, not just the default`() {
        val analytics = RecordingAnalyticsService()

        resolveInputMode(emptyArray(), analytics, boardSize = 5, hasInteractiveTerminal = terminal)

        assertEquals(5, analytics.events.single().properties["board_size"])
    }

    @Test
    fun `unrecognized arguments are reported instead of silently ignored`() {
        // audit finding on PR #20: a typo like "-keyboard" or "--Keyboard" previously fell
        // through to the MOUSE default with no feedback at all.
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--keyboard", "--bogus"), RecordingAnalyticsService(),
            warnUnrecognizedArg = { warnings.add(it) }, hasInteractiveTerminal = terminal,
        )

        assertEquals(InputMode.KEYBOARD, mode)
        assertEquals(listOf("--bogus"), warnings)
    }

    @Test
    fun `unrecognized arguments are reported even when the launch aborts`() {
        val warnings = mutableListOf<String>()

        resolveInputMode(
            arrayOf("--bogus"), RecordingAnalyticsService(),
            warnUnrecognizedArg = { warnings.add(it) }, reportNoInteractiveTerminal = {}, hasInteractiveTerminal = { false },
        )

        assertEquals(listOf("--bogus"), warnings)
    }

    @Test
    fun `--size=N, --keyboard and --mouse report no unrecognized arguments`() {
        val warnings = mutableListOf<String>()

        for (arg in listOf("--size=3", "--keyboard", "--mouse")) {
            resolveInputMode(arrayOf(arg), RecordingAnalyticsService(), warnUnrecognizedArg = { warnings.add(it) }, hasInteractiveTerminal = terminal)
        }

        assertEquals(emptyList(), warnings)
    }
}
