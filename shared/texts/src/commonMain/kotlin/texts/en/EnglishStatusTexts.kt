package texts.en

import texts.StatusTexts

object EnglishStatusTexts : StatusTexts {
    override fun saved(name: String) = "Saved as '$name'."
    override fun saveFailed(name: String) = "Couldn't save '$name'. The browser's storage may be full or turned off."
    override fun saveNameRejected(name: String) = "Save names can't be blank or contain slashes or '..', so '$name' wasn't saved. Please pick another name."
    override fun loaded(name: String) = "Loaded '$name'."
    override fun loadFailed(name: String) = "Couldn't load '$name'. Please try again."
    override fun saveDamaged(name: String) = "'$name' is damaged and can't be loaded. You can delete it in Load."
    override val saveFromNewerVersion = "This save was made by a newer version of the game. Please reload the page to update."

    override fun noSaveNamed(name: String, saves: String) = "No save named '$name'. $saves"
    override val couldNotListSaves = "Couldn't list your saves."
    override val noSavesAvailable = "No saves yet."
    override fun availableSaves(list: String) = "Your saves: $list"
    override fun lettersDontFit(side: Int) = "Letters don't fit a ${side}x$side board (26 letters, ${side * side} tiles). Please pick other labels."
    override fun cantBeSolved(save: String) = "No sequence of shifts can solve '$save'."
    override fun repaired(name: String) = "Repaired '$name' (two tiles swapped) and loaded it."
    override fun repairedNotUpdated(name: String) = "Repaired and loaded '$name', but couldn't update the save."

    override fun deleted(name: String) = "Deleted '$name'."
    override fun deletedNoneLeft(name: String) = "Deleted '$name'. No saves left."
    override fun alreadyDeleted(name: String) = "'$name' was already deleted."
    override fun couldNotDelete(name: String) = "Couldn't delete '$name'. Please try again."
    override fun saveNameHasSpaces(name: String) = "Save names can't have spaces, so '$name' wasn't saved. Please pick another name."
    override val linkCopied = "Link copied."
    override val copyFailed = "Couldn't copy the link. It's selected, so you can copy it yourself."
    override fun startedShared(side: Int) = "Started a shared ${side}x$side board."

    override fun linkUnsupportedSize(side: Int, min: Int, max: Int) =
        "This link's board is ${side}x$side, but the game plays ${min}x$min to ${max}x$max."
    override val linkUnsolvable = "No sequence of shifts can solve this link's board."
    override val linkInvalid = "This link is damaged, so its board can't be opened."
}
