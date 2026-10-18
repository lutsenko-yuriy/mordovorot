package web

import kotlinx.browser.document
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import viewmodel.UnsolvableSaveChoice
import web.drag.Axis
import web.drag.WrapPositions

/** Draws each [WebState] into [root] and turns clicks and drags into [game] intents (GH-110).
 *  Plain DOM, rebuilt every frame; the look comes in WU7. */
class DomRenderer(private val root: Element) : WebRenderer {

    /** Set once the game is built; only clicks read it, and none can come before then. */
    lateinit var game: WebGame

    /** The open Save dialog's warning line, updated in place while the player types. */
    private var saveWarning: Element? = null

    /** The pointer dragging a line, if any. [root] holds its capture, as the tiles are rebuilt. */
    private var dragPointer: Int? = null

    init {
        root.addEventListener("pointermove", { event ->
            val pointer = event as PointerEvent
            if (pointer.pointerId == dragPointer) game.onDragMove(pointer.clientX.toDouble(), pointer.clientY.toDouble())
        })
        root.addEventListener("pointerup", { endDrag(it as PointerEvent) { game.onDragEnd() } })
        root.addEventListener("pointercancel", { endDrag(it as PointerEvent) { game.onDragCancel() } })
        // After a pointerup the drag has already ended, so this only catches a capture lost mid-drag.
        root.addEventListener("lostpointercapture", { endDrag(it as PointerEvent) { game.onDragCancel() } })
    }

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

    /** The tiles framed by the arrows: ▲ above and ▼ below each column, ◀ and ▶ at each end of a
     *  row. A dragged line follows its offset, a tile pushed past one end showing at the other. */
    private fun board(state: WebState): Element {
        val side = state.side
        fun arrow(label: String, arrow: Arrow, line: Int) =
            button(label) { game.onArrow(arrow, line) }.also {
                it.disabled = !state.arrowsEnabled || state.dialog != null
                it.setAttribute("style", "width: ${TILE}px; height: ${TILE}px;")
            }
        fun arrows(arrow: Arrow, label: String, direction: String) = element("div").apply {
            setAttribute("style", "display: flex; flex-direction: $direction; gap: ${GAP}px;")
            repeat(side) { append(arrow(label, arrow, it)) }
        }

        val tiles = element("div", "tiles")
        tiles.setAttribute(
            "style",
            "position: relative; overflow: hidden; width: ${side * PITCH}px; height: ${side * PITCH}px;",
        )
        val drag = state.drag
        val wrapped = drag?.let { WrapPositions.of(side, it.tiles) }
        repeat(side) { row ->
            repeat(side) { col ->
                val value = state.tiles[row * side + col].toString()
                val x = if (drag?.axis == Axis.ROW && drag.line == row) wrapped!![col] else col.toDouble()
                val y = if (drag?.axis == Axis.COLUMN && drag.line == col) wrapped!![row] else row.toDouble()
                tiles.append(tile(value, x, y).apply {
                    addEventListener("pointerdown", { startDrag(it as PointerEvent, tiles, side, row, col) })
                })
                if (x > side - 1) tiles.append(tile(value, x - side, y))
                if (y > side - 1) tiles.append(tile(value, x, y - side))
            }
        }

        return element("div", "board").apply {
            setAttribute("style", "display: inline-grid; grid-template-columns: auto auto auto; gap: ${GAP}px;")
            append(
                element("span"), arrows(Arrow.UP, "▲", "row"), element("span"),
                arrows(Arrow.LEFT, "◀", "column"), tiles, arrows(Arrow.RIGHT, "▶", "column"),
                element("span"), arrows(Arrow.DOWN, "▼", "row"), element("span"),
            )
        }
    }

    /** A tile at column [x], row [y] of the board, in tiles. */
    private fun tile(value: String, x: Double, y: Double) = text("tile", value).apply {
        setAttribute(
            "style",
            "position: absolute; left: ${x * PITCH}px; top: ${y * PITCH}px; width: ${TILE}px; height: ${TILE}px; " +
                "display: flex; align-items: center; justify-content: center; background: #ddd; " +
                "user-select: none; touch-action: none; cursor: grab;",
        )
    }

    private fun startDrag(pointer: PointerEvent, tiles: Element, side: Int, row: Int, col: Int) {
        if (dragPointer != null || pointer.button.toInt() != 0) return
        pointer.preventDefault()
        dragPointer = pointer.pointerId
        root.setPointerCapture(pointer.pointerId)
        // Measured, so the drag follows the pointer at any zoom.
        val pitch = tiles.getBoundingClientRect().width / side
        game.onDragStart(row, col, pointer.clientX.toDouble(), pointer.clientY.toDouble(), pitch)
    }

    private fun endDrag(pointer: PointerEvent, end: () -> Unit) {
        if (pointer.pointerId != dragPointer) return
        dragPointer = null
        end()
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

    private companion object {
        const val TILE = 40
        const val GAP = 2
        const val PITCH = TILE + GAP
    }
}
