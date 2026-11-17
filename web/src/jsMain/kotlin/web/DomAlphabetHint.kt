package web

import org.w3c.dom.Element
import web.a11y.asList
import web.a11y.asListItem
import web.a11y.named

/** The alphabet hint's letters (GH-332) on a grid, each a list item a reader names as Read board does; a letter not on the board says so. */
internal fun alphabetGrid(dialog: WebDialog.AlphabetHint): Element = element("div", "alphabet").apply {
    asList(this)
    dialog.hint.letters.forEachIndexed { index, letter ->
        append(text(if (dialog.inUse(index)) "letter used" else "letter", letter).apply {
            asListItem(this, current = false)
            named(this, dialog.letterName(index))
        })
    }
}
