package texts.en

import texts.PageTexts

object EnglishPageTexts : PageTexts {
    override val toolbarNew = "New"
    override val toolbarSave = "Save"
    override val toolbarLoad = "Load"
    override val toolbarLabels = "Labels"
    override val toolbarShare = "Share"
    override val toolbarControls = "Controls"
    override val giveUp = "Give Up"
    override val mordiesSolution = "Mordie's solution"
    override val tutorial = "Tutorial"
    override val tutorialControls = "Controls"
    override val tutorialLabels = "Labels"
    override val tutorialSkip = "Skip"
    override val replayPrev = "◀ Prev"
    override val replayNext = "Next ▶"
    override val replayRestart = "↻ Restart"
    override fun movesCounter(moves: Int) = "Moves: $moves"

    override val mordieName = "Mordie:"
    override val solved = "Congratulations ✓"

    override fun footerBasedOn(version: String) = "v$version · Based on Loopover by "
    override val footerOriginal = "original"
    override fun footerUpdateNotice(version: String) = "v$version · New version available. "
    override val footerUpdate = "Update"

    override val labelNumbers = "Numbers"
    override val labelNumbersFrom0 = "Numbers from 0"
    override val labelPairs = "Pairs"
    override val labelPairsFrom0 = "Pairs from 0"
    override val labelLetters = "Letters"
    override val labelLetterPairs = "Letter pairs"
    override fun labelLimit(side: Int) = "up to ${side}x$side"
    override val layoutRightHanded = "Right-handed"
    override val layoutLeftHanded = "Left-handed"

    override val unsolvable = "unsolvable"
    override val stepRow = "Row"
    override val stepColumn = "Col"
}
