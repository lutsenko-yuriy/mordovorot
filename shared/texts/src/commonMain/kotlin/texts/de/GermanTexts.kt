package texts.de

import texts.Texts

/** German (GH-275): informal «du», Mordie keeps its name. */
object GermanTexts : Texts {
    override val mordie = GermanMordieTexts
    override val tutorial = GermanTutorialTexts
    override val dialogs = GermanDialogTexts
    override val status = GermanStatusTexts
    override val page = GermanPageTexts
    override val keys = GermanKeyTexts
    override val reader = GermanReaderTexts

    override fun moves(count: Int) = germanMoves(count)

    override fun boardSize(side: Int) = "${side}x$side"
}
