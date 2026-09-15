import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [InputMode] resolution (default mouse TUI, `--console` opt-in, the automatic
 * fallback to console mode when there is no interactive terminal - GH-3) and
 * [resolveInputMode]'s `app_launched` tracking.
 */
class InputModeTest {

    @Test
    fun `no arguments resolves to MOUSE`() {
        assertEquals(InputMode.MOUSE, InputMode.resolve(emptyArray(), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--console resolves to CONSOLE`() {
        assertEquals(InputMode.CONSOLE, InputMode.resolve(arrayOf("--console"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `MOUSE resolution falls back to CONSOLE when there is no interactive terminal`() {
        assertEquals(InputMode.CONSOLE, InputMode.resolve(emptyArray(), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `--console wins even without an interactive terminal`() {
        assertEquals(InputMode.CONSOLE, InputMode.resolve(arrayOf("--console"), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `--keyboard resolves to KEYBOARD`() {
        assertEquals(InputMode.KEYBOARD, InputMode.resolve(arrayOf("--keyboard"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--mouse resolves to MOUSE, same as no flag at all`() {
        assertEquals(InputMode.MOUSE, InputMode.resolve(arrayOf("--mouse"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--console wins over --keyboard when both are given`() {
        assertEquals(InputMode.CONSOLE, InputMode.resolve(arrayOf("--console", "--keyboard"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `KEYBOARD resolution falls back to CONSOLE when there is no interactive terminal`() {
        assertEquals(InputMode.CONSOLE, InputMode.resolve(arrayOf("--keyboard"), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `app_launched is tracked with the resolved mode`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveInputMode(arrayOf("--console"), analytics, boardSize = 4)

        assertEquals(InputMode.CONSOLE, mode)
        assertEquals(listOf(Event("app_launched", mapOf("mode" to "console", "board_size" to 4))), analytics.events)
    }

    @Test
    fun `app_launched carries the resolved board_size, not just the default`() {
        val analytics = RecordingAnalyticsService()

        resolveInputMode(arrayOf("--console"), analytics, boardSize = 5)

        assertEquals(listOf(Event("app_launched", mapOf("mode" to "console", "board_size" to 5))), analytics.events)
    }

    @Test
    fun `app_launched is tracked with keyboard when --keyboard is given`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveInputMode(arrayOf("--keyboard"), analytics, hasInteractiveTerminal = { true })

        assertEquals(InputMode.KEYBOARD, mode)
        assertEquals(listOf(Event("app_launched", mapOf("mode" to "keyboard", "board_size" to 4))), analytics.events)
    }

    @Test
    fun `--console and --keyboard together warn that --keyboard was ignored, but keep going`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(arrayOf("--console", "--keyboard"), analytics, warnConsoleKeyboardConflict = { warnings.add("--keyboard ignored") })

        assertEquals(InputMode.CONSOLE, mode)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `an explicit --keyboard silently downgraded to CONSOLE (no TTY) still warns, unlike before`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(
            arrayOf("--keyboard"), analytics,
            warnNoInteractiveTerminal = { warnings.add(it) },
            hasInteractiveTerminal = { false },
        )

        assertEquals(InputMode.CONSOLE, mode)
        assertEquals(listOf("--keyboard"), warnings)
    }

    @Test
    fun `--console alone falling back needs no such warning - it was the explicit request`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveInputMode(arrayOf("--console"), analytics, warnNoInteractiveTerminal = { warnings.add(it) }, hasInteractiveTerminal = { false })

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `unrecognized arguments are reported instead of silently ignored`() {
        // audit finding on PR #20: a typo like "-console" or "--Console" previously fell
        // through to the MOUSE default with no feedback at all.
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveInputMode(arrayOf("--console", "--bogus"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(InputMode.CONSOLE, mode)
        assertEquals(listOf("--bogus"), warnings)
    }

    @Test
    fun `--console alone reports no unrecognized arguments`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveInputMode(arrayOf("--console"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `--size=N is not reported as an unrecognized argument`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveInputMode(arrayOf("--size=3"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `--keyboard and --mouse alone report no unrecognized arguments`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveInputMode(arrayOf("--keyboard"), analytics, warnUnrecognizedArg = { warnings.add(it) })
        resolveInputMode(arrayOf("--mouse"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(emptyList(), warnings)
    }
}
