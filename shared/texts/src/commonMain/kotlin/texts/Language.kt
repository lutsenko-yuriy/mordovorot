package texts

import texts.de.GermanTexts
import texts.en.EnglishTexts
import texts.ru.RussianTexts

/** The languages the game speaks (GH-275). [code] goes in `lang`, the preference and analytics; [nativeName] is never translated. */
enum class Language(val code: String, val nativeName: String, val texts: Texts) {
    EN("en", "English", EnglishTexts),
    DE("de", "Deutsch", GermanTexts),
    RU("ru", "Русский", RussianTexts),
    ;

    companion object {
        fun fromCode(code: String?): Language? = entries.firstOrNull { it.code == code }

        /** The language of the browser's first tag (`de-AT` is German), `null` when it isn't one of ours: only the first counts, so `uk, ru` is none. */
        fun fromBrowser(tags: List<String>): Language? =
            tags.firstOrNull()?.substringBefore('-')?.substringBefore('_')?.lowercase()?.let(::fromCode)
    }
}
