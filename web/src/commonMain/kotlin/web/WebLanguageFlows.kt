package web

import texts.Language
import viewmodel.GameViewModel

/** What the player used to pick a language (GH-275); [wire] is `language_changed.control`. */
enum class PickerControl(val wire: String) { DROPDOWN("dropdown"), TOGGLES("toggles") }

/** The language picker's pick (GH-275). Ignored while a dialog, a toolbar action or a drag is under way, like every other control,
 *  so no text a dialog was worded with can go stale. */
internal class WebLanguageFlows(private val game: WebGame, private val viewModel: GameViewModel) {

    /** Switches to [language]: everything the frame words follows at once, but the status line, which was worded in the old language and is cleared. */
    fun onLanguage(language: Language, control: PickerControl) {
        if (!game.idle || language == viewModel.language()) return
        game.status = null
        viewModel.changeLanguage(language, control.wire)
        game.render()
    }
}
