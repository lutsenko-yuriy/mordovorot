package web.a11y

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import texts.texts
import viewmodel.TileSelection

// GH-167: during play, the tiles as a table a screen reader reads - a row element per board row,
// each tile its cell - and the hidden Read board button that says the whole board.
// GH-171: with a [TileSelection], a computer's board: a grid that is one Tab stop, the selected
// cell its active descendant, so NVDA and JAWS pass the letter keys on (D4, knowledge note). The
// solution screen's board is a Tab stop too, named after the current step.

private fun element(tag: String, cssClass: String? = null): Element =
    document.createElement(tag).apply { cssClass?.let { className = it } }

/** Its id, its own per cell: the same id moved along wouldn't tell a screen reader anything changed. */
internal fun cellId(row: Int, col: Int) = "tile-$row-$col"

/** The tile layer named as a table during play, or a grid with a [selection]; off play it reads as before.
 *  [board] is the layer's focus target. */
internal fun asTable(layer: Element, playing: Boolean, selection: TileSelection?, board: FocusTarget) = layer.apply {
    if (!playing) return@apply
    setAttribute("role", if (selection != null) "grid" else "table")
    setAttribute("aria-label", texts().reader.board)
    if (selection == null) return@apply
    setAttribute("tabindex", "0")
    activeCell(layer, selection)
    focusKey(board)
}

/** Points the grid [layer] at [selection]'s cell. */
internal fun activeCell(layer: Element, selection: TileSelection) =
    layer.setAttribute("aria-activedescendant", cellId(selection.row, selection.col))

/** Names the solution screen's board [layer] after the current step. */
internal fun nameBoard(layer: Element, name: String) = layer.setAttribute("aria-label", name)

/** The solution screen's board on a computer, a Tab stop for its keys, saying its [name] on focus. */
internal fun asSolutionBoard(layer: Element, name: String, board: FocusTarget) = layer.apply {
    setAttribute("role", "group")
    setAttribute("aria-label", name)
    setAttribute("tabindex", "0")
    focusKey(board)
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

/** [label] as the spans of a wave: the new one arriving, and the old one leaving (never read) unless there is none (GH-343). The new one is [read] when it is the tile's text. */
internal fun waveSpans(label: String, waving: Waving, read: Boolean): List<Element> = listOfNotNull(
    element("span", "label-in wave").apply {
        textContent = label
        setAttribute("style", "animation-delay: ${waving.arriveMs}ms")
        if (!read) setAttribute("aria-hidden", "true")
    },
    waving.old?.let { old ->
        hiddenFromReaders(element("span", "label-out wave").apply {
            textContent = old
            setAttribute("style", "animation-delay: ${waving.leaveMs}ms")
        })
    },
)

/** Writes [label] on [tile], if it changed: seen, and during play heard as [Announcements.spoken] has
 *  it, with `in place` when [home]. A [waving] label comes as the spans of a wave (GH-342). */
internal fun labelCell(tile: Element, label: String, playing: Boolean, home: Boolean, waving: Waving? = null) {
    val heard = if (playing) Announcements.spoken(label) + if (home) ", ${Announcements.IN_PLACE}" else "" else label
    val wave = waving?.id?.toString().orEmpty()
    if (tile.getAttribute("data-heard") == heard && tile.getAttribute("data-wave").orEmpty() == wave) return
    tile.setAttribute("data-heard", heard)
    if (waving == null) tile.removeAttribute("data-wave") else tile.setAttribute("data-wave", wave)
    tile.textContent = if (playing || waving != null) "" else label
    if (waving != null) tile.append(*waveSpans(label, waving, read = !playing).toTypedArray())
    if (playing) tile.append(
        *(if (waving == null) arrayOf<Element>(hiddenFromReaders(element("span").apply { textContent = label })) else arrayOf<Element>()),
        element("span", "visually-hidden").apply { textContent = heard },
    )
}

/** The second copy of a tile pushed past the board's edge: seen, not read, so a tile is read once. */
internal fun asCopy(tile: Element) = hiddenFromReaders(tile)

/** The Read board button, hidden from sight, saying the board's [words] as they are by then through
 *  [announcer]; disabled while a dialog is open, as the toolbar is. */
internal fun readBoardButton(words: () -> String?, dialogOpen: Boolean, announcer: DomAnnouncer?, focus: FocusTarget) =
    (element("button", "visually-hidden") as HTMLButtonElement).apply {
        textContent = texts().reader.readBoard
        addEventListener("click", { words()?.let { announcer?.say(it) } })
        disabled = dialogOpen
        focusKey(focus)
    }
