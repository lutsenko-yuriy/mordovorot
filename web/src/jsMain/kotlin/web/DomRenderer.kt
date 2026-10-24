package web

import kotlinx.browser.window
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.pointerevents.PointerEvent
import web.drag.Axis
import web.drag.LineOffset
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
    /** An animation frame is queued, so another slide or Mordie line doesn't queue a second. */
    private var framePending = false
    private var tiles: Element? = null

    private class Slide(val start: LineOffset, val startedAt: Double)

    /** Mordie's line, or its [ending] once solved. */
    private data class MordieLine(val text: String, val ending: Boolean)

    /** Mordie's lines come and go like chat messages. */
    private val feed = MessageFeed<MordieLine>(MORDIE_OUT_MS, MORDIE_IN_MS)

    /** This frame's Moves, Mordie's spot and toolbar: the feed measures how far its lines travel. */
    private var moves: Element? = null
    private var mordie: Element? = null
    private var toolbar: Element? = null

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
        val now = window.performance.now()
        // At once while the system asks for less motion, which can change with the tab open.
        val still = reducedMotion()
        feed.outMs = if (still) 0.0 else MORDIE_OUT_MS
        feed.inMs = if (still) 0.0 else MORDIE_IN_MS
        feed.show(state.mordie?.let { MordieLine(it, ending = state.solved) }, now)
        if (feed.moving(now)) schedule()
        val dialog = state.dialog
        // A rebuild would drop the name field's focus and caret, and nothing else changes while
        // the Save dialog is open, so typing only updates the warning.
        if (dialog is WebDialog.Save && saveWarning != null) {
            saveWarning?.textContent = dialog.warning ?: ""
            drawMordie(now)
            return
        }
        saveWarning = null
        // The rebuilt step list starts where this one was scrolled to.
        val stepsScroll = root.querySelector(".steps")?.scrollTop
        root.clear()
        root.style.setProperty("--side", state.side.toString())
        root.classList.toggle("solved", state.solved)
        root.classList.toggle("dragging", state.drag != null)
        root.classList.toggle("solution", state.solution != null)
        root.append(
            // The box the tall layout sizes the tiles from (GH-147).
            element("div", "stage").apply { append(board(state, justSolved = state.solved && before?.solved == false)) },
            *listOfNotNull(state.solution?.let { stepList(it) }).toTypedArray(),
            // Beside the board when the window is wide; when tall, its items stack around it (styles.css).
            element("div", "panel").apply {
                append(
                    text("moves", if (state.movesShown) "Moves: ${state.moves}" else "").also { moves = it },
                    element("div", "mordie").apply { append(element("div", "line")) }.also { mordie = it },
                    *listOfNotNull(state.solution?.let { replayButtons(it, { game }, dialog != null) }, giveUpRow(state.giveUp, { game }, dialog != null)).toTypedArray(),
                    element("div", "toolbar").also { toolbar = it }.apply {
                        append(
                            button("New") { game.onNew() }.also { it.disabled = dialog != null },
                            button("Save") { game.onSave() }.also { it.disabled = dialog != null },
                            button("Load") { game.onLoad() }.also { it.disabled = dialog != null },
                            button("Labels") { game.onLabels() }.also { it.disabled = dialog != null },
                        )
                    },
                    text("status", state.status ?: ""),
                )
            },
        )
        root.querySelector(".steps")?.let { steps ->
            // Back at the top on a Restart; otherwise the current step scrolls into view, if it must.
            steps.scrollTop = stepsScroll?.takeUnless { state.solution?.atStart == true } ?: 0.0
            (steps.querySelector(".current") as? HTMLElement)?.let { showStep(steps, it) }
        }
        // Only a dialog that has just opened fades in, not one redrawn as the player picks in it.
        val reopened = dialog != null && before?.dialog?.let { it::class == dialog::class } == true
        dialog?.let { root.append(element("div", if (reopened) "overlay shown" else "overlay").apply { append(dialogElement(it, game) { warning -> saveWarning = warning }) }) }
        drawMordie(now)
        root.querySelector("input")?.let { (it as HTMLInputElement).focus() }
        // The rebuilt Labels dialog keeps keyboard focus on the row just picked.
        if (reopened && dialog is WebDialog.Labels) (root.querySelector(".labels .checked") as? HTMLElement)?.focus()
    }

    /** Scrolls [steps] just enough to show [step]. Unlike `scrollIntoView`, never the page, so
     *  the buttons under the pointer stay put. */
    private fun showStep(steps: Element, step: HTMLElement) {
        val top = step.offsetTop.toDouble()
        val bottom = top + step.offsetHeight
        if (top < steps.scrollTop) steps.scrollTop = top
        if (bottom > steps.scrollTop + steps.clientHeight) steps.scrollTop = bottom - steps.clientHeight
    }

    /** Mordie's spot as the feed has it at [time]: a leaving line rises towards Moves; an arriving
     *  one rises from under the toolbar when it is beside Mordie (wide), else from just below. */
    private fun drawMordie(time: Double) {
        val spot = mordie ?: return
        val line = spot.firstElementChild as? HTMLElement ?: return
        val frame = feed.at(time)
        val message = frame.message
        if (line.getAttribute("data-text") != message?.toString()) {
            line.setAttribute("data-text", message.toString())
            line.clear()
            line.className = if (message?.ending == true) "line ending" else "line"
            when {
                message == null -> {}
                message.ending -> line.textContent = message.text
                else -> line.append(element("span", "name").apply { textContent = "Mordie:" }, " ${message.text}")
            }
        }
        val place = spot.getBoundingClientRect()
        val distance = when {
            frame.rise < 0 -> moves?.let { place.top - it.getBoundingClientRect().top } ?: 0.0
            frame.rise > 0 && window.matchMedia("(orientation: landscape)").matches ->
                toolbar?.let { it.getBoundingClientRect().bottom - place.top } ?: 0.0
            else -> place.height
        }
        line.style.transform = "translateY(${frame.rise * distance}px)"
        line.style.opacity = frame.alpha.toString()
    }

    /** Eases the line a shift moved into place: one tile after an arrow or a replay shift, the rest
     *  of the way after a drag. Any other change to the tiles, or a new drag, stops a slide under way. */
    private fun startSlide(before: WebState?, state: WebState) {
        val arrow = clicked
        val start = when {
            before == null || state.drag != null -> null
            arrow != null && state.tiles != before.tiles -> Slides.ofArrow(arrow.first, arrow.second)
            state.slide != null -> state.slide
            before.drag != null -> Slides.afterDrag(before.drag, moved = state.tiles != before.tiles, state.side)
            state.tiles != before.tiles -> null
            else -> return
        }
        val next = start?.takeUnless { reducedMotion() }?.let { Slide(it, window.performance.now()) }
        slide = next
        if (next != null) schedule()
    }

    private fun reducedMotion() = window.matchMedia("(prefers-reduced-motion: reduce)").matches

    private fun schedule() {
        if (framePending) return
        framePending = true
        window.requestAnimationFrame(::frame)
    }

    private fun frame(time: Double) {
        framePending = false
        slide?.let { running ->
            val offset = Slides.at(running.start, (time - running.startedAt) / SLIDE_MS)
            if (offset == null) slide = null
            val state = shown
            if (state != null) tiles?.let { drawTiles(it, state, offset) }
        }
        drawMordie(time)
        if (slide != null || feed.moving(time)) schedule()
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
        val texts = state.tileTexts
        layer.clear()
        repeat(side) { row ->
            repeat(side) { col ->
                val value = state.tiles[row * side + col]
                val label = texts[row * side + col]
                val x = if (offset?.axis == Axis.ROW && offset.line == row) wrapped!![col] else col.toDouble()
                val y = if (offset?.axis == Axis.COLUMN && offset.line == col) wrapped!![row] else row.toDouble()
                layer.append(tile(value, label, side, x, y).apply {
                    addEventListener("pointerdown", { startDrag(it as PointerEvent, layer, side, row, col) })
                })
                if (x > side - 1) layer.append(tile(value, label, side, x - side, y))
                if (y > side - 1) layer.append(tile(value, label, side, x, y - side))
            }
        }
    }

    /** Tile [value], showing [label], at column [x], row [y], in tiles; its colour comes from where it belongs. */
    private fun tile(value: Int, label: String, side: Int, x: Double, y: Double) = text("tile", label).apply {
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

    private companion object {
        const val SLIDE_MS = 160.0
        const val MORDIE_OUT_MS = 220.0
        const val MORDIE_IN_MS = 340.0
    }
}
