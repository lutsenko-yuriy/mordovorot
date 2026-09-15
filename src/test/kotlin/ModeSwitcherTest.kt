import presenter.ModeSwitchRequestedException
import presenter.ModeSwitcherImpl
import testing.FakeView
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Covers GH-30's `ModeSwitcherImpl`: TTY check, `input_mode_switched` tracking, throw vs
 *  reject-with-message vs same-mode no-op. */
class ModeSwitcherTest {

    @Test
    fun `switchTo a different mode with an interactive terminal throws ModeSwitchRequestedException and tracks success`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val switcher = ModeSwitcherImpl(
            view = FakeView(),
            currentMode = InputMode.MOUSE,
            analytics = analytics,
            hasInteractiveTerminal = { true },
        )

        val e = assertFailsWith<ModeSwitchRequestedException> { switcher.switchTo(InputMode.KEYBOARD, trigger = "toolbar") }

        assertEquals(InputMode.KEYBOARD, e.target)
        assertEquals(
            listOf(
                Event(
                    "input_mode_switched",
                    mapOf("from_mode" to "mouse", "to_mode" to "keyboard", "trigger" to "toolbar", "result" to "success"),
                ),
            ),
            analytics.events,
        )
    }

    @Test
    fun `switchTo console always succeeds regardless of terminal availability`(): Unit = runBlocking {
        val switcher = ModeSwitcherImpl(
            view = FakeView(),
            currentMode = InputMode.MOUSE,
            hasInteractiveTerminal = { false },
        )

        val e = assertFailsWith<ModeSwitchRequestedException> { switcher.switchTo(InputMode.CONSOLE, trigger = "toolbar") }

        assertEquals(InputMode.CONSOLE, e.target)
    }

    @Test
    fun `switchTo mouse or keyboard with no interactive terminal is rejected without throwing`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val switcher = ModeSwitcherImpl(
            view = view,
            currentMode = InputMode.CONSOLE,
            analytics = analytics,
            hasInteractiveTerminal = { false },
        )

        switcher.switchTo(InputMode.KEYBOARD, trigger = "command")

        assertEquals(1, view.shownMessages.size)
        assertEquals(true, view.shownMessages[0].contains("no interactive terminal", ignoreCase = true))
        assertEquals(
            listOf(
                Event(
                    "input_mode_switched",
                    mapOf("from_mode" to "console", "to_mode" to "keyboard", "trigger" to "command", "result" to "rejected_no_tty"),
                ),
            ),
            analytics.events,
        )

        switcher.switchTo(InputMode.MOUSE, trigger = "command")

        assertEquals(2, view.shownMessages.size)
        assertEquals(
            "rejected_no_tty",
            (analytics.events[1].properties["result"]),
        )
    }

    @Test
    fun `switchTo the current mode is a no-op - no exception, no message, no event`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val view = FakeView()
        val switcher = ModeSwitcherImpl(view = view, currentMode = InputMode.MOUSE, analytics = analytics)

        switcher.switchTo(InputMode.MOUSE, trigger = "toolbar")

        assertEquals(emptyList(), view.shownMessages)
        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `the trigger value is carried through unchanged for each caller`(): Unit = runBlocking {
        for (trigger in listOf("toolbar", "shortcut", "command")) {
            val analytics = RecordingAnalyticsService()
            val switcher = ModeSwitcherImpl(
                view = FakeView(),
                currentMode = InputMode.CONSOLE,
                analytics = analytics,
                hasInteractiveTerminal = { true },
            )

            assertFailsWith<ModeSwitchRequestedException> { switcher.switchTo(InputMode.MOUSE, trigger = trigger) }

            assertEquals(trigger, analytics.events.single().properties["trigger"])
        }
    }
}
