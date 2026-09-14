import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers [LaunchMode] resolution (default mouse TUI, `--console` opt-in, the automatic
 * fallback to console mode when there is no interactive terminal - GH-3) and
 * [resolveLaunchMode]'s `app_launched` tracking.
 */
class LaunchModeTest {

    @Test
    fun `no arguments resolves to MOUSE`() {
        assertEquals(LaunchMode.MOUSE, LaunchMode.resolve(emptyArray(), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--console resolves to CONSOLE`() {
        assertEquals(LaunchMode.CONSOLE, LaunchMode.resolve(arrayOf("--console"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `MOUSE resolution falls back to CONSOLE when there is no interactive terminal`() {
        assertEquals(LaunchMode.CONSOLE, LaunchMode.resolve(emptyArray(), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `--console wins even without an interactive terminal`() {
        assertEquals(LaunchMode.CONSOLE, LaunchMode.resolve(arrayOf("--console"), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `--keyboard resolves to KEYBOARD`() {
        assertEquals(LaunchMode.KEYBOARD, LaunchMode.resolve(arrayOf("--keyboard"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--mouse resolves to MOUSE, same as no flag at all`() {
        assertEquals(LaunchMode.MOUSE, LaunchMode.resolve(arrayOf("--mouse"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `--console wins over --keyboard when both are given`() {
        assertEquals(LaunchMode.CONSOLE, LaunchMode.resolve(arrayOf("--console", "--keyboard"), hasInteractiveTerminal = { true }))
    }

    @Test
    fun `KEYBOARD resolution falls back to CONSOLE when there is no interactive terminal`() {
        assertEquals(LaunchMode.CONSOLE, LaunchMode.resolve(arrayOf("--keyboard"), hasInteractiveTerminal = { false }))
    }

    @Test
    fun `app_launched is tracked with the resolved mode`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveLaunchMode(arrayOf("--console"), analytics)

        assertEquals(LaunchMode.CONSOLE, mode)
        assertEquals(listOf(Event("app_launched", mapOf("mode" to "console"))), analytics.events)
    }

    @Test
    fun `app_launched is tracked with keyboard when --keyboard is given`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveLaunchMode(arrayOf("--keyboard"), analytics, hasInteractiveTerminal = { true })

        assertEquals(LaunchMode.KEYBOARD, mode)
        assertEquals(listOf(Event("app_launched", mapOf("mode" to "keyboard"))), analytics.events)
    }

    @Test
    fun `--console and --keyboard together warn that --keyboard was ignored, but keep going`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveLaunchMode(arrayOf("--console", "--keyboard"), analytics, warnConsoleKeyboardConflict = { warnings.add("--keyboard ignored") })

        assertEquals(LaunchMode.CONSOLE, mode)
        assertEquals(1, warnings.size)
    }

    @Test
    fun `an explicit --keyboard silently downgraded to CONSOLE (no TTY) still warns, unlike before`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveLaunchMode(
            arrayOf("--keyboard"), analytics,
            warnNoInteractiveTerminal = { warnings.add(it) },
            hasInteractiveTerminal = { false },
        )

        assertEquals(LaunchMode.CONSOLE, mode)
        assertEquals(listOf("--keyboard"), warnings)
    }

    @Test
    fun `--console alone falling back needs no such warning - it was the explicit request`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveLaunchMode(arrayOf("--console"), analytics, warnNoInteractiveTerminal = { warnings.add(it) }, hasInteractiveTerminal = { false })

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `unrecognized arguments are reported instead of silently ignored`() {
        // audit finding on PR #20: a typo like "-console" or "--Console" previously fell
        // through to the MOUSE default with no feedback at all.
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        val mode = resolveLaunchMode(arrayOf("--console", "--bogus"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(LaunchMode.CONSOLE, mode)
        assertEquals(listOf("--bogus"), warnings)
    }

    @Test
    fun `--console alone reports no unrecognized arguments`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveLaunchMode(arrayOf("--console"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(emptyList(), warnings)
    }

    @Test
    fun `--keyboard and --mouse alone report no unrecognized arguments`() {
        val analytics = RecordingAnalyticsService()
        val warnings = mutableListOf<String>()

        resolveLaunchMode(arrayOf("--keyboard"), analytics, warnUnrecognizedArg = { warnings.add(it) })
        resolveLaunchMode(arrayOf("--mouse"), analytics, warnUnrecognizedArg = { warnings.add(it) })

        assertEquals(emptyList(), warnings)
    }
}
