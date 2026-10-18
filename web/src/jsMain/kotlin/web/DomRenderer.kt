package web

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import org.w3c.dom.pointerevents.PointerEvent
import viewmodel.UnsolvableSaveChoice
import web.drag.Axis
import web.drag.LineOffset
import web.drag.Slides
import web.drag.WrapPositions

/** Draws each [WebState] into [root] and turns clicks and drags into [game] intents (GH-110).
 *  Plain DOM, rebuilt every frame, with the look in `styles.css`. */
class DomRenderer(private val root: HTMLElement) : WebRenderer {

    /** Set once the game is built; only the page's events read it, and none can come before then. */
    lateinit var game: WebGame

    /** The open Save dialog's warning line, updated in place while the player types. */
    private var saveWarning: Element? = null

    /** The pointer dragging a line, if any. [root] holds its capture, as the tiles are rebuilt. */
    private var dragPointer: Int? = null

    private var shown: WebState? = null

    /** The arrow being clicked, so the frame its shift draws slides the line. */
    private var clicked: Pair<Arrow, Int>? = null

    /** The line easing into place, redrawn every animation frame until it gets there. */
    private var slide: Slide? = null
    private var tiles: Element? = null

    private class Slide(val start: LineOffset, val startedAt: Double)

    init {
        root.className = "app"
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
        val before = shown
        shown = state
        startSlide(before, state)
        val dialog = state.dialog
        // A rebuild would drop the name field's focus and caret, and nothing else changes while
        // the Save dialog is open, so typing only updates the warning.
        if (dialog is WebDialog.Save && saveWarning != null) {
            saveWarning?.textContent = dialog.warning ?: ""
            return
        }
        saveWarning = null
        root.clear()
        root.style.setProperty("--side", state.side.toString())
        root.classList.toggle("solved", state.solved)
        root.classList.toggle("dragging", state.drag != null)
        root.append(
            board(state, justSolved = state.solved && before?.solved == false),
            // Beside the board when the window is wide; when tall, its items stack around it (styles.css).
            element("div", "panel").apply {
                append(
                    text("moves", "Moves: ${state.moves}"),
                    text("mordie", state.mordie?.let { if (state.solved) it else "Mordie: $it" } ?: ""),
                    element("div", "toolbar").apply {
                        append(
                            button("New") { game.onNew() }.also { it.disabled = dialog != null },
                            button("Save") { game.onSave() }.also { it.disabled = dialog != null },
                            button("Load") { game.onLoad() }.also { it.disabled = dialog != null },
                        )
                    },
                    text("status", state.status ?: ""),
                )
            },
        )
        dialog?.let { root.append(element("div", "overlay").apply { append(dialog(it)) }) }
        root.querySelector("input")?.let { (it as HTMLInputElement).focus() }
    }

    /** Eases the line a shift moved into place: one tile after an arrow, the rest of the way after a
     *  drag. Any other change to the tiles, or a new drag, stops a slide under way. */
    private fun startSlide(before: WebState?, state: WebState) {
        val arrow = clicked
        val start = when {
            before == null || state.drag != null -> null
            arrow != null && state.tiles != before.tiles -> Slides.ofArrow(arrow.first, arrow.second)
            before.drag != null -> Slides.afterDrag(before.drag, moved = state.tiles != before.tiles, state.side)
            state.tiles != before.tiles -> null
            else -> return
        }
        val reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches
        val next = start?.takeUnless { reduced }?.let { Slide(it, window.performance.now()) }
        if (next != null && slide == null) window.requestAnimationFrame(::frame)
        slide = next
    }

    private fun frame(time: Double) {
        val running = slide ?: return
        val offset = Slides.at(running.start, (time - running.startedAt) / SLIDE_MS)
        if (offset == null) slide = null else window.requestAnimationFrame(::frame)
        val state = shown ?: return
        tiles?.let { drawTiles(it, state, offset) }
    }

    /** The current slide's offset, before its next animation frame. */
    private fun slideOffset(): LineOffset? =
        slide?.let { Slides.at(it.start, (window.performance.now() - it.startedAt) / SLIDE_MS) }

    /** The tiles framed by the arrows: ▲ above and ▼ below each column, ◀ and ▶ at each end of a row. */
    private fun board(state: WebState, justSolved: Boolean): Element {
        val side = state.side
        val disabled = !state.arrowsEnabled || state.dialog != null
        fun arrow(label: String, arrow: Arrow, line: Int, name: String) =
            button(label, "arrow") {
                clicked = arrow to line
                try {
                    game.onArrow(arrow, line)
                } finally {
                    clicked = null
                }
            }.also {
                it.disabled = disabled
                it.setAttribute("aria-label", name)
            }
        fun arrows(arrow: Arrow, label: String, axis: String, name: (Int) -> String) =
            element("div", "arrows $axis").apply { repeat(side) { append(arrow(label, arrow, it, name(it + 1))) } }

        val layer = element("div", "tiles")
        tiles = layer
        drawTiles(layer, state, state.drag ?: slideOffset())

        val classes = listOfNotNull("board", "solved".takeIf { state.solved }, "just-solved".takeIf { justSolved })
        return element("div", classes.joinToString(" ")).apply {
            append(
                element("span"), arrows(Arrow.UP, "▲", "row") { "Column $it up" }, element("span"),
                arrows(Arrow.LEFT, "◀", "column") { "Row $it left" },
                layer,
                arrows(Arrow.RIGHT, "▶", "column") { "Row $it right" },
                element("span"), arrows(Arrow.DOWN, "▼", "row") { "Column $it down" }, element("span"),
            )
        }
    }

    /** Fills [layer] with the board's tiles, the line [offset] names moved by it: a tile pushed past one
     *  end shows at the other as well, and the layer clips both. */
    private fun drawTiles(layer: Element, state: WebState, offset: LineOffset?) {
        val side = state.side
        val wrapped = offset?.let { WrapPositions.of(side, it.tiles) }
        layer.clear()
        repeat(side) { row ->
            repeat(side) { col ->
                val value = state.tiles[row * side + col]
                val x = if (offset?.axis == Axis.ROW && offset.line == row) wrapped!![col] else col.toDouble()
                val y = if (offset?.axis == Axis.COLUMN && offset.line == col) wrapped!![row] else row.toDouble()
                layer.append(tile(value, side, x, y).apply {
                    addEventListener("pointerdown", { startDrag(it as PointerEvent, layer, side, row, col) })
                })
                if (x > side - 1) layer.append(tile(value, side, x - side, y))
                if (y > side - 1) layer.append(tile(value, side, x, y - side))
            }
        }
    }

    /** Tile [value] at column [x], row [y], in tiles; its colour comes from where it belongs. */
    private fun tile(value: Int, side: Int, x: Double, y: Double) = text("tile", value.toString()).apply {
        setAttribute(
            "style",
            "--x: $x; --y: $y; --row: ${(value - 1) / side}; --col: ${(value - 1) % side};",
        )
    }

    private fun startDrag(pointer: PointerEvent, tiles: Element, side: Int, row: Int, col: Int) {
        if (dragPointer != null || pointer.button.toInt() != 0) return
        pointer.preventDefault()
        dragPointer = pointer.pointerId
        root.setPointerCapture(pointer.pointerId)
        // Measured, so the drag follows the pointer at any tile size or zoom.
        val pitch = tiles.getBoundingClientRect().width / side
        game.onDragStart(row, col, pointer.clientX.toDouble(), pointer.clientY.toDouble(), pitch)
    }

    private fun endDrag(pointer: PointerEvent, end: () -> Unit) {
        if (pointer.pointerId != dragPointer) return
        dragPointer = null
        end()
    }

    private fun dialog(dialog: WebDialog): Element = element("div", "dialog").apply {
        setAttribute("role", "dialog")
        setAttribute("aria-modal", "true")
        when (dialog) {
            is WebDialog.Size -> append(
                text("title", "New game size"),
                choices(dialog.sizes.map { size -> button("${size}x$size") { game.onSizeChosen(size) } }),
                actions(button("Cancel", "secondary") { game.onSizeChosen(null) }),
            )
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    setAttribute("aria-label", "Save name")
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
                    text("title", "Save game"),
                    name,
                    warning,
                    actions(
                        button("Cancel", "secondary") { game.onSaveAnswered(null) },
                        button("Save") { game.onSaveAnswered(name.value) },
                    ),
                )
            }
            is WebDialog.Load -> {
                append(text("title", if (dialog.restore) "Restore a saved game?" else "Load game"))
                if (dialog.saves.isEmpty()) append(text("message", "No saves found."))
                else append(choices(dialog.saves.map { save -> button(save.label) { game.onLoadChosen(save.name) } }, "list"))
                append(actions(button("Cancel", "secondary") { game.onLoadChosen(null) }))
            }
            is WebDialog.Unsolvable -> append(
                text("title", "Can't be solved"),
                text("message", dialog.message),
                actions(
                    button("New game", "secondary") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) },
                    button("Repair & load") { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) },
                ),
            )
        }
    }

    private fun choices(buttons: List<Element>, kind: String? = null) =
        element("div", listOfNotNull("choices", kind).joinToString(" ")).apply { buttons.forEach { append(it) } }

    private fun actions(vararg buttons: Element) = element("div", "actions").apply { append(*buttons) }

    private fun element(tag: String, cssClass: String? = null): Element =
        document.createElement(tag).apply { cssClass?.let { className = it } }

    private fun text(cssClass: String, content: String) = element("div", cssClass).apply { textContent = content }

    private fun button(label: String, cssClass: String? = null, onClick: () -> Unit) =
        (element("button", cssClass) as HTMLButtonElement).apply {
            textContent = label
            addEventListener("click", { onClick() })
        }

    private companion object {
        const val SLIDE_MS = 160.0
    }
}
