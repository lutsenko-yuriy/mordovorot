package texts.en

import texts.StatusTexts

object EnglishStatusTexts : StatusTexts {
    override fun saved(name: String) = "Saved as '$name'."
    override fun saveFailed(name: String, reason: String) = "Could not save as '$name': $reason"
    override fun loaded(name: String) = "Loaded '$name'."
    override fun loadFailed(name: String) = "Could not load '$name'."

    override fun noSaveNamed(name: String, saves: String) = "No save named '$name'. $saves"
    override val couldNotListSaves = "Could not list available saves."
    override val noSavesAvailable = "No saves available."
    override fun availableSaves(list: String) = "Available saves: $list"
    override fun lettersDontFit(side: Int) = "Letters don't fit a ${side}x$side board (26 letters, ${side * side} tiles) - pick other labels."
    override fun cantBeSolved(save: String) = "'$save' can't be solved - no sequence of shifts reaches the solved board."
    override fun cantBeSolvedDialog(save: String) = "'$save' can't be solved by any sequence of shifts."
    override fun repaired(name: String) = "Repaired '$name' (two tiles swapped) and loaded it."
    override fun repairedNotUpdated(name: String, reason: String) = "Repaired and loaded '$name', but could not update the save file: $reason"

    override fun deleted(name: String) = "Deleted '$name'."
    override fun deletedNoneLeft(name: String) = "Deleted '$name'. No saves left."
    override fun alreadyDeleted(name: String) = "No save named '$name' - it was already deleted."
    override fun couldNotDelete(name: String) = "Could not delete '$name'."
    override fun saveNameHasSpaces(name: String) = "'$name' isn't a usable save name (no spaces) - not saved."
    override val linkCopied = "Link copied."
    override val copyFailed = "Couldn't copy the link - it's selected, so copy it yourself."
    override fun startedShared(side: Int) = "Started a shared ${side}x$side board."

    override fun linkUnsupportedSize(side: Int, min: Int, max: Int) =
        "This link's board is ${side}x$side; this site plays ${min}x$min to ${max}x$max."
    override val linkUnsolvable = "No sequence of shifts can solve this link's board."
    override val linkInvalid = "This link's board isn't a valid board."
}
