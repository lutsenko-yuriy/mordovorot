package texts

/** The status line's messages, [save] being a save's `name (NxN)`. [reason] is a developer diagnostic passed through. */
interface StatusTexts {
    fun saved(name: String): String
    fun saveFailed(name: String, reason: String): String
    fun loaded(name: String): String
    fun loadFailed(name: String): String

    /** [saves] is [availableSaves] or one of its two failure sentences. */
    fun noSaveNamed(name: String, saves: String): String
    val couldNotListSaves: String
    val noSavesAvailable: String
    fun availableSaves(list: String): String
    fun lettersDontFit(side: Int): String
    fun cantBeSolved(save: String): String
    fun cantBeSolvedDialog(save: String): String
    fun repaired(name: String): String
    fun repairedNotUpdated(name: String, reason: String): String

    fun deleted(name: String): String
    fun deletedNoneLeft(name: String): String
    fun alreadyDeleted(name: String): String
    fun couldNotDelete(name: String): String
    fun saveNameHasSpaces(name: String): String
    val linkCopied: String
    val copyFailed: String
    fun startedShared(side: Int): String

    /** Why a share link's board can't be opened. */
    fun linkUnsupportedSize(side: Int, min: Int, max: Int): String
    val linkUnsolvable: String
    val linkInvalid: String
}
