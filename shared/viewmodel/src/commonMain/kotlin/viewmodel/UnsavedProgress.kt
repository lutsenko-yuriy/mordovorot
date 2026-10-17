package viewmodel

/** Whether the current game has shifts no save holds yet (GH-110) - the web's leave-site
 *  prompt. Set by a shift; cleared by a new game, a successful save or load, and a repair.
 *  One per session, like [CurrentSaveName]; the terminal never reads it. */
class UnsavedProgress {
    var unsaved: Boolean = false
}
