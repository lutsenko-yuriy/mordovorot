package web.scenario

import kotlin.test.Test

/** GH-275: the language picker's events; every other event is the same in every language. */
class WebLanguageAnalyticsScenarioTest {
    @Test
    fun `switching sends language_changed`() = webTest {
        // TODO: 1. Start in a German browser and play
        // TODO: 2. Pick Russian
        // TODO: 3. The `language_changed` event is sent, with `from: de`, `to: ru` and `from_source: browser` (GH-275)
    }

    @Test
    fun `launch reports the language`() = webTest {
        // TODO: 1. Start in a Russian browser
        // TODO: 2. `app_launched` carries `language: ru` (GH-275)
    }

    @Test
    fun `analytics don't depend on the language`() = webTest {
        // TODO: 1. Play the same scripted game (new game, shifts, save, load, labels, give up, replay, share) in en, de and ru
        // TODO: 2. The three recorded event lists are equal, except for `app_launched.language`
    }
}
