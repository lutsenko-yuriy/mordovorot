package texts

private var active = Language.EN

/** The texts in the player's language (GH-275): English until [useLanguage]; every caller goes through here. */
fun texts(): Texts = active.texts

fun activeLanguage(): Language = active

/** Makes [language] the one every [texts] call speaks: the session's, set by the viewmodel's `SessionLanguage`. */
fun useLanguage(language: Language) {
    active = language
}
