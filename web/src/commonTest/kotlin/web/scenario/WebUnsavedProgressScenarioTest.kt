package web.scenario

import kotlin.test.Test

/** GH-110 scenarios: the unsaved-progress flag behind the leave-site prompt (the prompt itself is a
 *  DOM `beforeunload` check in WU7's checklist). Same setup as [WebStartupScenarioTest]. */
class WebUnsavedProgressScenarioTest {

    @Test
    fun `unsaved progress is set by a shift and cleared by save, load and New game`() {
        // WU5
        // TODO: 1. Fresh game: false.
        // TODO: 2. After a shift: true.
        // TODO: 3. After a successful save: false.
        // TODO: 4. Shift again: true; after a successful load: false.
        // TODO: 5. Shift, then New with a size: false.
    }

    @Test
    fun `a failed or cancelled save or load keeps the progress unsaved`() {
        // WU5
        // TODO: 1. Shift; Save with a rejected name: still true.
        // TODO: 2. Cancel Save: still true.
        // TODO: 3. Cancel Load: still true.
    }

    @Test
    fun `Repair & load clears unsaved progress`() {
        // WU5
        // TODO: 1. Shift; load "odd", choose Repair & load: false.
    }
}
