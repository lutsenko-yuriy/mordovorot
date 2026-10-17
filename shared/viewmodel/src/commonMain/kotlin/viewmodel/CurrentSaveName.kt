package viewmodel

/** The name the current game was last loaded from or saved to (GH-99) - prefills the Save
 *  dialog. One per session, so it survives mode switches; never persisted. */
class CurrentSaveName {
    var name: String? = null
}
