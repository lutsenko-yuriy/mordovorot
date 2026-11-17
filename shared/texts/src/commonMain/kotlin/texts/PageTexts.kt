package texts

/** The page's own words: toolbar, buttons, counters, footer, labels and layout titles. */
interface PageTexts {
    /** The browser tab's title. */
    val title: String

    /** The language picker's accessible name (GH-275). */
    val language: String

    val toolbarNew: String
    val toolbarSave: String
    val toolbarLoad: String
    val toolbarLabels: String
    val toolbarShare: String
    val toolbarControls: String
    val giveUp: String

    /** The `[ Hint ]` button that opens the alphabet of the letter tile labels (GH-332). */
    val alphabetHint: String
    val mordiesSolution: String
    val tutorial: String
    val tutorialControls: String
    val tutorialLabels: String
    val tutorialSkip: String
    val replayPrev: String
    val replayNext: String
    val replayRestart: String
    fun movesCounter(moves: Int): String

    /** Mordie's name before his line, with its colon. */
    val mordieName: String
    val solved: String

    /** The footer's credit and update notice, led by `v<version> · `. */
    fun footerBasedOn(version: String): String
    val footerOriginal: String
    fun footerUpdateNotice(version: String): String
    val footerUpdate: String

    val labelNumbers: String
    val labelNumbersFrom0: String
    val labelPairs: String
    val labelPairsFrom0: String
    val labelLetters: String
    val labelLetterPairs: String
    val labelCyrillicLetters: String
    val labelCyrillicLetterPairs: String
    val labelGreekLetters: String
    val labelGreekLetterPairs: String

    /** The Tile labels dialog's group headers (GH-323). */
    val labelGroupNumbers: String
    val labelGroupLatin: String
    val labelGroupCyrillic: String
    val labelGroupGreek: String

    /** A scheme's row inside a letter group, whose header already names the alphabet (GH-323): `Letters`; its pair is `Pairs`. */
    val labelRowLetters: String

    /** A closed group's header when it holds the marked scheme, for screen readers (GH-323): `Latin letters, marked: Pairs`. */
    fun labelGroupMarked(group: String, row: String): String

    /** The labels dialog's sample for a scheme that doesn't fit: `up to 5x5`. */
    fun labelLimit(side: Int): String
    val layoutRightHanded: String
    val layoutLeftHanded: String

    /** Added to a save that no sequence of shifts can solve. */
    val unsolvable: String
    val stepRow: String
    val stepColumn: String
}
