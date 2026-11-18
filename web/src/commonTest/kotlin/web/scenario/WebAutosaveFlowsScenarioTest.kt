package web.scenario

import kotlin.test.Test

/** GH-338: Update, a share link and the tutorial replay try an autosave first and ask only if it fails; every scenario is made green in WU1. */
class WebAutosaveFlowsScenarioTest {
    @Test
    fun `Update with moves autosaves and reloads without asking`() = webTest {
        // TODO: 1. A new version is ready
        // TODO: 2. Make moves
        // TODO: 3. Press Update
        // TODO: 4. The autosave slot is written
        // TODO: 5. The page reloads with no dialog
        // TODO: 6. The `autosave_written` event says `trigger: update`
    }

    @Test
    fun `Update asks first when the autosave fails`() = webTest {
        // TODO: 1. Make the store refuse writes
        // TODO: 2. A new version is ready and the player has moves
        // TODO: 3. Press Update
        // TODO: 4. `Update to the new version?` shows
    }

    @Test
    fun `a share link opened mid-game autosaves and starts at once`() = webTest {
        // TODO: 1. Make moves
        // TODO: 2. Paste a share link into the open tab
        // TODO: 3. The autosave slot is written
        // TODO: 4. The shared board starts with no dialog
        // TODO: 5. The `autosave_written` event says `trigger: shared_link`
    }

    @Test
    fun `a share link asks first when the autosave fails`() = webTest {
        // TODO: 1. Make the store refuse writes
        // TODO: 2. Make moves
        // TODO: 3. Paste a share link into the open tab
        // TODO: 4. `Open the shared board?` shows
    }

    @Test
    fun `replaying the tutorial autosaves and starts at once`() = webTest {
        // TODO: 1. Make moves
        // TODO: 2. Press `[ Tutorial ]`
        // TODO: 3. The autosave slot is written
        // TODO: 4. The tutorial starts with no dialog
        // TODO: 5. The `autosave_written` event says `trigger: tutorial_replay`
    }

    @Test
    fun `replaying the tutorial asks first when the autosave fails`() = webTest {
        // TODO: 1. Make the store refuse writes
        // TODO: 2. Make moves
        // TODO: 3. Press `[ Tutorial ]`
        // TODO: 4. `Save your game first?` shows
    }
}
