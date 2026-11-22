package texts

/** Dialog titles, messages, buttons and accessible names. */
interface DialogTexts {
    val sizeTitle: String
    val saveTitle: String
    val loadTitle: String
    val restoreTitle: String
    fun deleteTitle(name: String): String
    val unsolvableTitle: String
    val labelsTitle: String
    val giveUpTitle: String
    val controlsTitle: String

    /** The alphabet hint's title by alphabet (GH-332). */
    val alphabetLatin: String
    val alphabetCyrillic: String
    val alphabetGreek: String
    val alphabetPrereform: String
    val linkRejectedTitle: String
    val openSharedTitle: String
    fun sharedLabelsTitle(labels: String): String
    val shareTitle: String
    val updateTitle: String
    val replaySaveTitle: String

    val veryLargeTitle: String
    val veryLargeWarning: String

    val cancel: String
    val ok: String
    val save: String
    val dontSave: String
    val delete: String
    val close: String
    val open: String
    val update: String
    val yes: String
    val no: String
    val newGame: String
    val repairAndLoad: String
    val copyLink: String
    val copied: String

    val noSaves: String
    val deleteMessage: String
    val openSharedMessage: String
    val updateMessage: String
    val sharedLabelsMessage: String
    val replaySaveMessage: String
    /** The same question before a restart (GH-349); its title is [replaySaveTitle]. */
    val restartSaveMessage: String
    fun overwriteWarning(name: String): String

    /** The Share dialog's note, the link starting from the same board or from the board as it is now. */
    fun shareNoteSame(side: Int): String
    fun shareNoteNow(side: Int): String
    fun shareLabels(labels: String): String
    val shareProgress: String
    fun shareProgressSolving(moves: Int): String
    fun shareProgressSolved(moves: Int): String
    val shareProgressGaveUp: String
    fun shareProgressGaveUpAfter(moves: Int): String

    /** A Load row's trash button. */
    fun deleteName(name: String): String

    val saveNameName: String
    val shareLinkName: String
    val tileLabelsName: String
    val keyboardKeysName: String
}
