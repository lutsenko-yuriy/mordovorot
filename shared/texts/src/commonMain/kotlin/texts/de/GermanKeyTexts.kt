package texts.de

import texts.KeyTexts

object GermanKeyTexts : KeyTexts {
    override val hintSelect = "Auswahl"
    override val hintRow = "Zeile"
    override val hintColumn = "Spalte"
    override val hintPrevNext = "zurück/weiter"
    override val hintNext = "weiter"
    override val hintRestart = "Neustart"
    override val hintScroll = "scrollen"

    override fun letterOrArrow(letter: String, arrow: String) = "$letter oder $arrow"

    override val rightHandedSelect = "Kachel wählen    W A S D"
    override val rightHandedShiftRow = "Zeile schieben   ← →  H L"
    override val rightHandedShiftColumn = "Spalte schieben  ↑ ↓  K J"
    override val leftHandedSelect = "Kachel wählen    ← ↑ ↓ →  H J K L"
    override val leftHandedShiftRow = "Zeile schieben   A D"
    override val leftHandedShiftColumn = "Spalte schieben  W S"
    override val toolbarLine1 = "Neu 1 · Speichern 2 · Laden 3 · Beschriftung 4"
    override val toolbarLine2 = "Aufgeben 5 · Steuerung 6 · Teilen 7 · Tutorial 8"
    override val toolbarLine3 = "0 Brett vorlesen (Screenreader)"
    override val solutionLine1 = "Lösung: ← → A D H L zurück/weiter ·"
    override val solutionLine2 = "  Leertaste/Enter weiter · R Neustart ·"
    override val solutionLine3 = "  ↑ ↓ W S K J scrollen"
}
