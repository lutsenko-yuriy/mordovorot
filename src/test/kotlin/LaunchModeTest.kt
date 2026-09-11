import kotlin.test.Test

/**
 * Scenario stubs for GH-3's launch-mode resolution: default mouse TUI, `--console` opt-in,
 * and the automatic fallback to console mode when there is no interactive terminal. Filled
 * in by `implement` during WU1, once `LaunchMode` exists.
 */
class LaunchModeTest {

    @Test
    fun `no arguments resolves to MOUSE`() {
        // TODO: Resolve LaunchMode from an empty argument array, with a real-terminal check stubbed true
        // TODO: Verify the result is LaunchMode.MOUSE
    }

    @Test
    fun `--console resolves to CONSOLE`() {
        // TODO: Resolve LaunchMode from arrayOf("--console")
        // TODO: Verify the result is LaunchMode.CONSOLE, regardless of the terminal check
    }

    @Test
    fun `MOUSE resolution falls back to CONSOLE when there is no interactive terminal`() {
        // TODO: Resolve LaunchMode from an empty argument array, with the real-terminal check stubbed false
        // TODO: Verify the result is LaunchMode.CONSOLE
    }

    @Test
    fun `app_launched is tracked with the resolved mode`() {
        // TODO: Resolve LaunchMode for each case above through Main's startup path
        // TODO: Verify RecordingAnalyticsService recorded app_launched{mode} matching the resolution
    }
}
