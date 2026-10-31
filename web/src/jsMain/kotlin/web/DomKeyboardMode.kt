package web

import org.w3c.dom.Element
import viewmodel.TileSelection
import web.a11y.hiddenFromReaders

// GH-171: what keyboard mode draws beside the board, apart from how a screen reader reads it.

/** The rounded rectangles around [selection]'s row and column, and the thicker ring on its tile. Seen, not read. */
internal fun selectionOutlines(selection: TileSelection) = element("div", "selection").apply {
    hiddenFromReaders(this)
    placeOutlines(this, selection)
    append(element("div", "selected-row"), element("div", "selected-col"), element("div", "selected-tile"))
}

internal fun placeOutlines(outlines: Element, selection: TileSelection) =
    outlines.setAttribute("style", "--selected-row: ${selection.row}; --selected-col: ${selection.col};")

/** The key hints under the board: the keys above what they do. */
internal fun keyHints(hints: List<KeyHint>) = element("div", "key-hints").apply {
    for (hint in hints) append(element("div", "key-hint").apply { append(text("keys", hint.keys), text("does", hint.does)) })
}
