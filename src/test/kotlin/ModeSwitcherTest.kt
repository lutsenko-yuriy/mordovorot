import kotlin.test.Test

/**
 * Covers GH-30's `ModeSwitcher`/`ModeSwitcherImpl`: the single place that checks for an
 * interactive terminal, tracks `input_mode_switched`, and either throws
 * `ModeSwitchRequestedException` (success) or shows a rejection message and returns normally
 * (no interactive terminal for a mouse/keyboard target) - mirroring
 * `BasePresenter.exitGame`'s "usually throws, returns normally on failure" contract.
 */
class ModeSwitcherTest {

    @Test
    fun `switchTo a different mode with an interactive terminal throws ModeSwitchRequestedException and tracks success`() {
        // TODO: 1. Build ModeSwitcherImpl with hasInteractiveTerminal = { true }, current mode MOUSE,
        //          a RecordingAnalyticsService.
        // TODO: 2. Call switchTo(KEYBOARD, trigger = "toolbar").
        // TODO: 3. Verify it throws ModeSwitchRequestedException(KEYBOARD).
        // TODO: 4. Verify analytics.events contains input_mode_switched with from_mode=mouse,
        //          to_mode=keyboard, trigger=toolbar, result=success.
    }

    @Test
    fun `switchTo console always succeeds regardless of terminal availability`() {
        // TODO: 1. Build ModeSwitcherImpl with hasInteractiveTerminal = { false }, current mode MOUSE.
        // TODO: 2. Call switchTo(CONSOLE, trigger = "toolbar").
        // TODO: 3. Verify it throws ModeSwitchRequestedException(CONSOLE) (console never needs a TTY).
    }

    @Test
    fun `switchTo mouse or keyboard with no interactive terminal is rejected without throwing`() {
        // TODO: 1. Build ModeSwitcherImpl with hasInteractiveTerminal = { false }, current mode CONSOLE,
        //          a FakeView.
        // TODO: 2. Call switchTo(KEYBOARD, trigger = "command") - verify it returns normally (no exception).
        // TODO: 3. Verify view.shownMessages contains a message naming the rejection (no interactive terminal).
        // TODO: 4. Verify analytics.events contains input_mode_switched with result=rejected_no_tty.
        // TODO: 5. Repeat for switchTo(MOUSE, ...).
    }

    @Test
    fun `switchTo the current mode is a no-op - no exception, no message, no event`() {
        // TODO: 1. Build ModeSwitcherImpl with current mode MOUSE.
        // TODO: 2. Call switchTo(MOUSE, trigger = "toolbar").
        // TODO: 3. Verify it returns normally, view.shownMessages is empty, analytics.events is empty.
    }

    @Test
    fun `the trigger value is carried through unchanged for each caller`() {
        // TODO: 1. For each of "toolbar", "shortcut", "command": call switchTo with an interactive
        //          terminal and verify the tracked event's trigger property matches.
    }
}
