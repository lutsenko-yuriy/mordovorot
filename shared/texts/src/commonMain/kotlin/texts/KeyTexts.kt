package texts

/** The words around the keys: hints under the board and the Controls dialog's key list. The keys themselves stay in code. */
interface KeyTexts {
    val hintSelect: String
    val hintRow: String
    val hintColumn: String
    val hintPrevNext: String
    val hintNext: String
    val hintRestart: String
    val hintScroll: String

    /** `H or ←`. */
    fun letterOrArrow(letter: String, arrow: String): String

    val rightHandedSelect: String
    val rightHandedShiftRow: String
    val rightHandedShiftColumn: String
    val leftHandedSelect: String
    val leftHandedShiftRow: String
    val leftHandedShiftColumn: String
    val toolbarLine1: String
    val toolbarLine2: String
    val toolbarLine3: String
    val toolbarLine4: String
    val solutionLine1: String
    val solutionLine2: String
    val solutionLine3: String
}
