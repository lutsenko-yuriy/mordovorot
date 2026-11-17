package web

import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLSelectElement
import texts.Language
import web.a11y.asPickerGroup
import web.a11y.asToggle
import web.a11y.hiddenFromReaders
import web.a11y.named

/** The language picker in the page's top right corner (GH-275), in [host] outside `#app`, which this keeps rather than rebuilds so
 *  focus stays on the pick. Both forms are built; language.css shows the `EN ▾` dropdown on phones and the `EN DE RU` toggles elsewhere. */
internal class DomLanguagePicker(private val host: Element, private val onPick: (Language, PickerControl) -> Unit) {

    private val toggles = Language.entries.associateWith { language ->
        element("button", "language-toggle").apply {
            textContent = language.code.uppercase()
            setAttribute("title", language.nativeName)
            setAttribute("lang", language.code)
            setAttribute("data-focus", "language-${language.code}")
            addEventListener("click", { onPick(language, PickerControl.TOGGLES) })
        }
    }

    private val current = element("span", "language-current")

    private val select = (element("select") as HTMLSelectElement).apply {
        setAttribute("data-focus", "language-select")
        Language.entries.forEach { language ->
            append(element("option").apply {
                textContent = language.nativeName
                setAttribute("value", language.code)
                setAttribute("lang", language.code)
            })
        }
        addEventListener("change", { Language.fromCode(value)?.let { onPick(it, PickerControl.DROPDOWN) } })
    }

    init {
        host.clear()
        host.append(
            element("div", "language-toggles").apply { toggles.values.forEach { append(it) } },
            element("div", "language-dropdown").apply {
                append(hiddenFromReaders(current), select)
            },
        )
    }

    /** Shows [language] as the pick, in place; [name] is the picker's accessible name, in that language. */
    fun show(language: Language, name: String) {
        asPickerGroup(host, name)
        named(select, name)
        select.value = language.code
        current.textContent = "${language.code.uppercase()} ▾"
        toggles.forEach { (each, button) -> asToggle(button, each == language) }
    }

    companion object {
        /** The element the page keeps for the picker, between the game and the footer in tab order. */
        fun create(): HTMLElement = element("div", "language-picker") as HTMLElement
    }
}
