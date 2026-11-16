package texts.ru

import texts.Texts

/** Russian (GH-275): informal «ты», Mordie is `Морди`. */
object RussianTexts : Texts {
    override val mordie = RussianMordieTexts
    override val tutorial = RussianTutorialTexts
    override val dialogs = RussianDialogTexts
    override val status = RussianStatusTexts
    override val page = RussianPageTexts
    override val keys = RussianKeyTexts
    override val reader = RussianReaderTexts

    override fun moves(count: Int) = russianMoves(count)

    override fun boardSize(side: Int) = "${side}x$side"
}
