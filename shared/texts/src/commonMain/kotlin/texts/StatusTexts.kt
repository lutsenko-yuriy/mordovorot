package texts

/** The status line's messages, [save] being a save's `name (NxN)`. Storage's own reasons are never shown (GH-274). */
interface StatusTexts {
    fun saved(name: String): String
    fun saveFailed(name: String): String
    fun saveNameRejected(name: String): String
    fun loaded(name: String): String
    fun loadFailed(name: String): String
    fun saveDamaged(name: String): String

    /** A save with a first character no version here knows (GH-313): a newer build wrote it. */
    val saveFromNewerVersion: String

    /** [saves] is [availableSaves] or one of its two failure sentences. */
    fun noSaveNamed(name: String, saves: String): String
    val couldNotListSaves: String
    val noSavesAvailable: String
    fun availableSaves(list: String): String
    fun lettersDontFit(side: Int): String
    fun cyrillicLettersDontFit(side: Int): String
    fun cantBeSolved(save: String): String
    fun repaired(name: String): String
    fun repairedNotUpdated(name: String): String

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
