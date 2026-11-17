package texts.de

import texts.PageTexts

object GermanPageTexts : PageTexts {
    override val title = "Mordovorot"
    override val language = "Sprache"

    override val toolbarNew = "Neu"
    override val toolbarSave = "Speichern"
    override val toolbarLoad = "Laden"
    override val toolbarLabels = "Beschriftung"
    override val toolbarShare = "Teilen"
    override val toolbarControls = "Steuerung"
    override val giveUp = "Aufgeben"
    override val alphabetHint = "Hinweis"
    override val mordiesSolution = "Mordies Lösung"
    override val tutorial = "Tutorial"
    override val tutorialControls = "Steuerung"
    override val tutorialLabels = "Beschriftung"
    override val tutorialSkip = "Überspringen"
    override val replayPrev = "◀ Zurück"
    override val replayNext = "Weiter ▶"
    override val replayRestart = "↻ Neustart"
    override fun movesCounter(moves: Int) = "Züge: $moves"

    override val mordieName = "Mordie:"
    override val solved = "Glückwunsch ✓"

    override fun footerBasedOn(version: String) = "v$version · Basiert auf Loopover von "
    override val footerOriginal = "Original"
    override fun footerUpdateNotice(version: String) = "v$version · Neue Version verfügbar. "
    override val footerUpdate = "Aktualisieren"

    override val labelNumbers = "Zahlen"
    override val labelNumbersFrom0 = "Zahlen ab 0"
    override val labelPairs = "Paare"
    override val labelPairsFrom0 = "Paare ab 0"
    override val labelLetters = "Lateinische Buchstaben"
    override val labelLetterPairs = "Lateinische Buchstabenpaare"
    override val labelCyrillicLetters = "Kyrillische Buchstaben"
    override val labelCyrillicLetterPairs = "Kyrillische Buchstabenpaare"
    override val labelGreekLetters = "Griechische Buchstaben"
    override val labelGreekLetterPairs = "Griechische Buchstabenpaare"
    override val labelGroupNumbers = "Zahlen"
    override val labelGroupLatin = "Lateinische Buchstaben"
    override fun labelGroupMarked(group: String, row: String) = "$group, markiert: $row"
    override val labelRowLetters = "Buchstaben"
    override val labelGroupCyrillic = "Kyrillische Buchstaben"
    override val labelGroupGreek = "Griechische Buchstaben"
    override fun labelLimit(side: Int) = "bis ${side}x$side"
    override val layoutRightHanded = "Für Rechtshänder"
    override val layoutLeftHanded = "Für Linkshänder"

    override val unsolvable = "nicht lösbar"
    override val stepRow = "Zeile"
    override val stepColumn = "Spalte"
}
