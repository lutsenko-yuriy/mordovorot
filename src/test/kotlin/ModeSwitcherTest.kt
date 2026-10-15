import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking
import testing.RecordingAnalyticsService
import testing.RecordingAnalyticsService.Event
import viewmodel.ModeSwitchRequestedException
import viewmodel.ModeSwitcherImpl

/** Covers GH-30's `ModeSwitcherImpl`: `input_mode_switched` tracking, throw vs same-mode no-op. */
class ModeSwitcherTest {

    @Test
    fun `switchTo a different mode throws ModeSwitchRequestedException and tracks success`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val switcher = ModeSwitcherImpl(currentMode = InputMode.MOUSE, analytics = analytics)

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
    fun `switchTo the current mode is a no-op - no exception, no event`(): Unit = runBlocking {
        val analytics = RecordingAnalyticsService()
        val switcher = ModeSwitcherImpl(currentMode = InputMode.MOUSE, analytics = analytics)

        switcher.switchTo(InputMode.MOUSE, trigger = "toolbar")

        assertEquals(emptyList(), analytics.events)
    }

    @Test
    fun `the trigger value is carried through unchanged for each caller`(): Unit = runBlocking {
        for (trigger in listOf("toolbar", "shortcut")) {
            val analytics = RecordingAnalyticsService()
            val switcher = ModeSwitcherImpl(currentMode = InputMode.KEYBOARD, analytics = analytics)

            assertFailsWith<ModeSwitchRequestedException> { switcher.switchTo(InputMode.MOUSE, trigger = trigger) }

            assertEquals(trigger, analytics.events.single().properties["trigger"])
        }
    }
}
