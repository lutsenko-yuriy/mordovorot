package web

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement

/** Draws each [WebState] into [root] and turns clicks into [game] intents (GH-110). Plain DOM,
 *  rebuilt every frame; the look comes in WU7. */
class DomRenderer(private val root: Element) : WebRenderer {

    /** Set once the game is built; only clicks read it, and none can come before then. */
    lateinit var game: WebGame

    override fun render(state: WebState) {
        root.clear()
        root.append(
            text("moves", "Moves: ${state.moves}"),
            text("mordie", state.mordie?.let { if (state.solved) it else "Mordie: $it" } ?: ""),
            board(state),
            element("div", "toolbar").apply { append(button("New") { game.onNew() }) },
            text("status", state.status ?: ""),
        )
        state.dialog?.let { root.append(dialog(it)) }
    }

    /** The tiles in a grid framed by the arrows: ▲ above and ▼ below each column, ◀ and ▶ at
     *  each end of a row. */
    private fun board(state: WebState): Element {
        val side = state.side
        val grid = element("div", "board")
        grid.setAttribute("style", "display: grid; grid-template-columns: repeat(${side + 2}, 2.5em); gap: 2px;")
        fun arrow(label: String, arrow: Arrow, line: Int) =
            button(label) { game.onArrow(arrow, line) }.also { it.disabled = !state.arrowsEnabled }
        fun blank() = element("span")

        grid.append(blank())
        repeat(side) { grid.append(arrow("▲", Arrow.UP, it)) }
        grid.append(blank())
        repeat(side) { row ->
            grid.append(arrow("◀", Arrow.LEFT, row))
            repeat(side) { col -> grid.append(text("tile", state.tiles[row * side + col].toString())) }
            grid.append(arrow("▶", Arrow.RIGHT, row))
        }
        grid.append(blank())
        repeat(side) { grid.append(arrow("▼", Arrow.DOWN, it)) }
        grid.append(blank())
        return grid
    }

    private fun dialog(dialog: WebDialog): Element = element("div", "dialog").apply {
        when (dialog) {
            is WebDialog.Size -> {
                append(text("title", "New game size"))
                dialog.sizes.forEach { size -> append(button("${size}x$size") { game.onSizeChosen(size) }) }
                append(button("Cancel") { game.onSizeChosen(null) })
            }
        }
    }

    private fun element(tag: String, cssClass: String? = null): Element =
        document.createElement(tag).apply { cssClass?.let { className = it } }

    private fun text(cssClass: String, content: String) = element("div", cssClass).apply { textContent = content }

    private fun button(label: String, onClick: () -> Unit) = (element("button") as HTMLButtonElement).apply {
        textContent = label
        addEventListener("click", { onClick() })
    }
}
