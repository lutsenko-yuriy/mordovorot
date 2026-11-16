package texts

/**
 * Every text the player reads or hears (GH-274), one interface per area. A member's name is its key.
 * A text with a number or a name is a function of an `Int` or a `String`, never preformatted, so a
 * language can inflect it itself. English is [texts.en.EnglishTexts]; [texts] picks the language.
 */
interface Texts {
    val mordie: MordieTexts
    val tutorial: TutorialTexts
    val dialogs: DialogTexts
    val status: StatusTexts
    val page: PageTexts
    val keys: KeyTexts
    val reader: ReaderTexts

    /** `1 move`, `2 moves`. */
    fun moves(count: Int): String

    /** `4x4`. */
    fun boardSize(side: Int): String
}
