package web

import org.w3c.dom.Element
import viewmodel.TileSelection

// GH-167: during play, the tiles as a table a screen reader reads - a row element per board row,
// each tile its cell - and the hidden Read board button that says the whole board.
// GH-171: with a [TileSelection], keyboard mode's board: a grid that is one Tab stop, the selected
// cell its active descendant, so NVDA and JAWS pass the letter keys on (D4, knowledge note). The
// solution screen's board is a Tab stop too, named after the current step.

/** Its id, its own per cell: the same id moved along wouldn't tell a screen reader anything changed. */
internal fun cellId(row: Int, col: Int) = "tile-$row-$col"

/** The tile layer named as a table during play, or a grid with a [selection]; off play it reads as before. */
internal fun asTable(layer: Element, playing: Boolean, selection: TileSelection?) = layer.apply {
    if (!playing) return@apply
    setAttribute("role", if (selection != null) "grid" else "table")
    setAttribute("aria-label", "Board")
    if (selection == null) return@apply
    setAttribute("tabindex", "0")
    setAttribute("aria-activedescendant", cellId(selection.row, selection.col))
    focusKey(Focus.Board)
}

/** The solution screen's board in keyboard mode, a Tab stop for its keys, saying its [name] on focus. */
internal fun asSolutionBoard(layer: Element, name: String) = layer.apply {
    setAttribute("role", "group")
    setAttribute("aria-label", name)
    setAttribute("tabindex", "0")
    focusKey(Focus.Board)
}

/** A board row's wrapper in [layer]. It has no box of its own, so its tiles still position against
 *  the layer. */
internal fun gridRow(layer: Element, playing: Boolean): Element =
    element("div", "tile-row").also {
        if (playing) it.setAttribute("role", "row")
        layer.append(it)
    }

/** [tile] as its row's cell during play. With a [gridCell] id it's a grid's cell. */
internal fun asCell(tile: Element, playing: Boolean, gridCell: String?) = tile.apply {
    if (!playing) return@apply
    setAttribute("role", if (gridCell != null) "gridcell" else "cell")
    gridCell?.let { id = it }
}

/** Writes [label] on [tile], if it changed: seen, and during play heard as [Announcements.spoken] has
 *  it, with `in place` when [home]. */
internal fun labelCell(tile: Element, label: String, playing: Boolean, home: Boolean) {
    val heard = if (playing) Announcements.spoken(label) + if (home) ", ${Announcements.IN_PLACE}" else "" else label
    if (tile.getAttribute("data-heard") == heard) return
    tile.setAttribute("data-heard", heard)
    tile.textContent = if (playing) "" else label
    if (playing) tile.append(
        element("span").apply {
            textContent = label
            setAttribute("aria-hidden", "true")
        },
        element("span", "visually-hidden").apply { textContent = heard },
    )
}

/** The rounded rectangles around [selection]'s row and column, and the thicker ring on its tile. Seen, not read. */
internal fun selectionOutlines(selection: TileSelection) = element("div", "selection").apply {
    setAttribute("aria-hidden", "true")
    placeOutlines(this, selection)
    append(element("div", "selected-row"), element("div", "selected-col"), element("div", "selected-tile"))
}

internal fun placeOutlines(outlines: Element, selection: TileSelection) =
    outlines.setAttribute("style", "--selected-row: ${selection.row}; --selected-col: ${selection.col};")

/** The key hints under the board: the keys above what they do. */
internal fun keyHints(hints: List<KeyHint>) = element("div", "key-hints").apply {
    for (hint in hints) append(element("div", "key-hint").apply { append(text("keys", hint.keys), text("does", hint.does)) })
}

/** The second copy of a tile pushed past the board's edge: seen, not read, so a tile is read once. */
internal fun asCopy(tile: Element) = tile.apply { setAttribute("aria-hidden", "true") }

/** The Read board button, hidden from sight, saying the board's [words] as they are by then through
 *  [announcer]; disabled while a dialog is open, as the toolbar is. */
internal fun readBoardButton(words: () -> String?, dialogOpen: Boolean, announcer: DomAnnouncer?) =
    button("Read board", "visually-hidden") { words()?.let { announcer?.say(it) } }.focusKey(Focus.ReadBoard).also {
        it.disabled = dialogOpen
    }
