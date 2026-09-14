import kotlin.test.Test

/**
 * Covers GH-30's session loop: `GameSession` rebuilding the View/presenter/analytics stack
 * around the same `BoardModel`/`SaveRepository` instances every time `play()` unwinds with a
 * `ModeSwitchRequestedException`, and ending cleanly when `play()` returns normally instead.
 */
class GameSessionTest {

    @Test
    fun `switching modes preserves board state and does not reshuffle`() {
        // TODO: 1. Build a GameSession with a FakeBoardModel seeded to a known non-solved arrangement.
        // TODO: 2. Run the session starting in console mode; the console play() throws
        //          ModeSwitchRequestedException(MOUSE) on its first call.
        // TODO: 3. Verify the same FakeBoardModel instance is passed to the rebuilt mouse-mode presenter.
        // TODO: 4. Verify board.calls contains no reset/shuffle call across the switch.
    }

    @Test
    fun `switching modes skips the startup restore prompt on sessions after the first`() {
        // TODO: 1. Seed a FakeSaveRepository with one save file.
        // TODO: 2. Run GameSession starting in console mode with startupRestoreDone = false for
        //          session 1; console play() immediately throws ModeSwitchRequestedException(KEYBOARD).
        // TODO: 3. Verify the rebuilt keyboard-mode presenter is constructed with startupRestoreDone = true.
        // TODO: 4. Verify the startup restore prompt view call fires at most once total, on session 1 only.
    }

    @Test
    fun `three consecutive switches cycle through console, mouse, and keyboard, each rebuilding a fresh View-presenter`() {
        // TODO: 1. Configure a view/presenter factory spy that records which mode it was asked to build.
        // TODO: 2. Drive: console play() throws ModeSwitchRequestedException(MOUSE) -> mouse play()
        //          throws ModeSwitchRequestedException(KEYBOARD) -> keyboard play() returns normally (exit).
        // TODO: 3. Verify the factory was called exactly 3 times, in order [CONSOLE, MOUSE, KEYBOARD].
        // TODO: 4. Verify GameSession.run() itself returns normally after the third call (no further rebuild).
    }

    @Test
    fun `play returning normally (exit) ends the session loop without another rebuild`() {
        // TODO: 1. Run GameSession starting in mouse mode; play() returns normally (simulating exit).
        // TODO: 2. Verify the view/presenter factory was invoked exactly once.
        // TODO: 3. Verify GameSession.run() returns without throwing.
    }
}
