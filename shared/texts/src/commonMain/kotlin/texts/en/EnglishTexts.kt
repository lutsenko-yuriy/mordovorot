package texts.en

import texts.Texts

/** English, the source language (GH-274). */
object EnglishTexts : Texts {
    override val mordie = EnglishMordieTexts
    override val tutorial = EnglishTutorialTexts
    override val dialogs = EnglishDialogTexts
    override val status = EnglishStatusTexts
    override val page = EnglishPageTexts
    override val keys = EnglishKeyTexts
    override val reader = EnglishReaderTexts

    override fun moves(count: Int) = englishMoves(count)

    override fun boardSize(side: Int) = "${side}x$side"
}

internal fun englishMoves(count: Int) = if (count == 1) "1 move" else "$count moves"
