package web

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import viewmodel.UnsolvableSaveChoice

/** Draws each [WebState] into [root] and turns clicks into [game] intents (GH-110). Plain DOM,
 *  rebuilt every frame; the look comes in WU7. */
class DomRenderer(private val root: Element) : WebRenderer {

    /** Set once the game is built; only clicks read it, and none can come before then. */
    lateinit var game: WebGame

    /** The open Save dialog's warning line, updated in place while the player types. */
    private var saveWarning: Element? = null

    override fun render(state: WebState) {
        val dialog = state.dialog
        // A rebuild would drop the name field's focus and caret, and nothing else changes while
        // the Save dialog is open, so typing only updates the warning.
        if (dialog is WebDialog.Save && saveWarning != null) {
            saveWarning?.textContent = dialog.warning ?: ""
            return
        }
        saveWarning = null
        root.clear()
        root.append(
            text("moves", "Moves: ${state.moves}"),
            text("mordie", state.mordie?.let { if (state.solved) it else "Mordie: $it" } ?: ""),
            board(state),
            element("div", "toolbar").apply {
                append(
                    button("New") { game.onNew() }.also { it.disabled = dialog != null },
                    button("Save") { game.onSave() }.also { it.disabled = dialog != null },
                    button("Load") { game.onLoad() }.also { it.disabled = dialog != null },
                )
            },
            text("status", state.status ?: ""),
        )
        dialog?.let { root.append(dialog(it)) }
        root.querySelector("input")?.let { (it as HTMLInputElement).focus() }
    }

    /** The tiles in a grid framed by the arrows: ▲ above and ▼ below each column, ◀ and ▶ at
     *  each end of a row. */
    private fun board(state: WebState): Element {
        val side = state.side
        val grid = element("div", "board")
        grid.setAttribute("style", "display: grid; grid-template-columns: repeat(${side + 2}, 2.5em); gap: 2px;")
        fun arrow(label: String, arrow: Arrow, line: Int) =
            button(label) { game.onArrow(arrow, line) }.also { it.disabled = !state.arrowsEnabled || state.dialog != null }
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
            is WebDialog.Save -> {
                append(text("title", "Save game"))
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    addEventListener("input", { game.onSaveNameTyped(value) })
                    addEventListener("keydown", {
                        // An Enter that confirms an IME composition isn't the player's Save.
                        val key = it as KeyboardEvent
                        if (key.key == "Enter" && !key.isComposing && key.keyCode != 229) game.onSaveAnswered(value)
                    })
                }
                val warning = text("warning", dialog.warning ?: "")
                saveWarning = warning
                append(
                    name,
                    warning,
                    button("Save") { game.onSaveAnswered(name.value) },
                    button("Cancel") { game.onSaveAnswered(null) },
                )
            }
            is WebDialog.Load -> {
                append(text("title", if (dialog.restore) "Restore a saved game?" else "Load game"))
                if (dialog.saves.isEmpty()) append(text("message", "No saves found."))
                dialog.saves.forEach { save -> append(button(save.label) { game.onLoadChosen(save.name) }) }
                append(button("Cancel") { game.onLoadChosen(null) })
            }
            is WebDialog.Unsolvable -> append(
                text("title", "Can't be solved"),
                text("message", dialog.message),
                button("Repair & load") { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) },
                button("New game") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) },
            )
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
