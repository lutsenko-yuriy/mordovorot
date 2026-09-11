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
    fun `app_launched is tracked with the resolved mode`() {
        val analytics = RecordingAnalyticsService()

        val mode = resolveLaunchMode(arrayOf("--console"), analytics)

        assertEquals(LaunchMode.CONSOLE, mode)
        assertEquals(listOf(Event("app_launched", mapOf("mode" to "console"))), analytics.events)
    }
}
