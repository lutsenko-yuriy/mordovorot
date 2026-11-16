package web.scenario

import kotlin.test.Test

/** GH-275: the game follows the browser's language, a picker switches it, and a switch keeps the game. */
class WebLanguageScenarioTest {
    @Test
    fun `a German browser starts the game in German`() = webTest {
        // TODO: 1. Start with browser language `de-DE` and no preference
        // TODO: 2. The page texts are German
        // TODO: 3. The picker shows `DE` as the current language
        // TODO: 4. No preference is written
    }

    @Test
    fun `an unsupported browser language starts in English`() = webTest {
        // TODO: 1. Start with browser language `fr`
        // TODO: 2. The page texts are English
    }

    @Test
    fun `only the browser's first language counts`() = webTest {
        // TODO: 1. Start with browser languages `uk, ru`
        // TODO: 2. The page texts are English
    }

    @Test
    fun `a stored language beats the browser`() = webTest {
        // TODO: 1. Seed the preference `ru`, browser `de`
        // TODO: 2. The page texts are Russian
    }

    @Test
    fun `an unknown stored language falls back to detection`() = webTest {
        // TODO: 1. Seed the preference `xx`, browser `de`
        // TODO: 2. The page texts are German
    }

    @Test
    fun `switching mid-game keeps the game`() = webTest {
        // TODO: 1. Make 2 moves, with a selected tile and Mordie searching
        // TODO: 2. Pick German
        // TODO: 3. Board, moves, selection and the running search are unchanged
        // TODO: 4. Buttons, key hints, footer and Mordie's line are German
        // TODO: 5. The status line is cleared
    }

    @Test
    fun `a pick is remembered`() = webTest {
        // TODO: 1. Pick German
        // TODO: 2. The `language` preference is `de`
        // TODO: 3. A reload starts in German
    }

    @Test
    fun `picking the current language does nothing`() = webTest {
        // TODO: 1. Pick English while in English
        // TODO: 2. No event, no preference write and no change to the page
    }

    @Test
    fun `switching on the solution screen re-words it`() = webTest {
        // TODO: 1. Give up on a board
        // TODO: 2. Pick German
        // TODO: 3. Mordie's line and the step list are German, and the replay position is unchanged
    }

    @Test
    fun `switching on a solved board re-words the ending`() = webTest {
        // TODO: 1. Solve a board
        // TODO: 2. Pick German
        // TODO: 3. The ending text is German
    }

    @Test
    fun `switching during the tutorial keeps the step`() = webTest {
        // TODO: 1. Open the tutorial at step 2
        // TODO: 2. Pick German
        // TODO: 3. The bubble is German, and the step and its progress are unchanged
    }

    @Test
    fun `a pick while a dialog is open is ignored`() = webTest {
        // TODO: 1. Open the Save dialog
        // TODO: 2. Try to pick German
        // TODO: 3. The language stays, and the dialog is unchanged
    }

    @Test
    fun `a save and a share link carry no language`() = webTest {
        // TODO: 1. Save a game in German with the name `mein Spiel`
        // TODO: 2. Switch to English and load it: English texts, the typed name
        // TODO: 3. The share link has no language part
    }

    @Test
    fun `Russian keeps Latin letters and NATO words`() = webTest {
        // TODO: 1. Pick Russian and the Letters scheme
        // TODO: 2. The tiles show Latin letters
        // TODO: 3. Read board says `delta`, `bravo charlie`
    }

    @Test
    fun `a failed preference write still switches the session`() = webTest {
        // TODO: 1. Make the preference store throw
        // TODO: 2. Pick German
        // TODO: 3. The page is German for the session
    }
}
