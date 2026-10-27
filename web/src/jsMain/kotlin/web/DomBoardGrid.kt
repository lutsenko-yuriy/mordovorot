package web

import org.w3c.dom.Element

// GH-167: during play, the tiles as a table a screen reader reads - a row element per board row,
// each tile its cell - and the hidden Read board button that says the whole board.

/** The tile layer named as a table during play; off play it reads as before. */
internal fun asTable(layer: Element, playing: Boolean) = layer.apply {
    if (!playing) return@apply
    setAttribute("role", "table")
    setAttribute("aria-label", "Board")
}

/** A board row's wrapper in [layer]. It has no box of its own, so its tiles still position against
 *  the layer. */
internal fun gridRow(layer: Element, playing: Boolean): Element =
    element("div", "tile-row").also {
        if (playing) it.setAttribute("role", "row")
        layer.append(it)
    }

/** [tile] as its row's cell during play: [label] is seen, and heard as [Announcements.spoken] has
 *  it, with `in place` when [home]. */
internal fun asCell(tile: Element, label: String, playing: Boolean, home: Boolean) = tile.apply {
    if (!playing) return@apply
    setAttribute("role", "cell")
    textContent = ""
    val heard = Announcements.spoken(label) + if (home) ", ${Announcements.IN_PLACE}" else ""
    append(
        element("span").apply {
            textContent = label
            setAttribute("aria-hidden", "true")
        },
        element("span", "visually-hidden").apply { textContent = heard },
    )
}

/** The second copy of a tile pushed past the board's edge: seen, not read, so a tile is read once. */
internal fun asCopy(tile: Element) = tile.apply { setAttribute("aria-hidden", "true") }

/** The Read board button, hidden from sight, saying [words] through [announcer]; disabled while a
 *  dialog is open, as the toolbar is. */
internal fun readBoardButton(words: String, dialogOpen: Boolean, announcer: DomAnnouncer?) =
    button("Read board", "visually-hidden") { announcer?.say(words) }.focusKey(Focus.ReadBoard).also {
        it.disabled = dialogOpen
    }
