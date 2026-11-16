package texts

import texts.en.EnglishTexts

/** The texts in the player's language: English for now (GH-274); every caller goes through here. */
fun texts(): Texts = EnglishTexts
