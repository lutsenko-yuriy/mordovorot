package texts.en

import texts.KeyTexts

object EnglishKeyTexts : KeyTexts {
    override val hintSelect = "select"
    override val hintRow = "row"
    override val hintColumn = "column"
    override val hintPrevNext = "prev/next"
    override val hintNext = "next"
    override val hintRestart = "restart"
    override val hintScroll = "scroll"

    override fun letterOrArrow(letter: String, arrow: String) = "$letter or $arrow"

    override val rightHandedSelect = "Select tile    W A S D"
    override val rightHandedShiftRow = "Shift row      ← →  H L"
    override val rightHandedShiftColumn = "Shift column   ↑ ↓  K J"
    override val leftHandedSelect = "Select tile    ← ↑ ↓ →  H J K L"
    override val leftHandedShiftRow = "Shift row      A D"
    override val leftHandedShiftColumn = "Shift column   W S"
    override val toolbarLine1 = "New 1 · Save 2 · Load 3 · Labels 4"
    override val toolbarLine2 = "Give Up 5 · Hint 8 · Tutorial 9"
    override val toolbarLine3 = "0 Read board (screen reader)"
    override val toolbarLine4 = "Controls 6 · Share 7 · Restart R"
    override val solutionLine1 = "Solution: ← → A D H L Prev/Next ·"
    override val solutionLine2 = "  Space/Enter Next · R Restart ·"
    override val solutionLine3 = "  ↑ ↓ W S K J scroll"
}
