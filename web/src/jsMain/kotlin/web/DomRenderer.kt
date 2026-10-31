package web

import kotlinx.browser.window
import kotlinx.dom.clear
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import web.a11y.DomAnnouncer
import web.a11y.applyFocus
import web.a11y.focusKey
import web.a11y.focusedKey
import web.a11y.setBehindDialog
import web.drag.LineOffset

/** Draws each [WebState] into [root] and turns its clicks into [game] intents, the board's and drags to [DomBoard] (GH-110).
 *  Plain DOM, rebuilt every frame, but for keyboard mode's board where [WebState.keepsBoardFrom] says so (GH-171), with the look in the sheets `index.html` links. */
class DomRenderer(private val root: HTMLElement, private val announcer: DomAnnouncer?) : WebRenderer {

    /** Set once the game is built; only the page's events read it, and none can come before then. */
    lateinit var game: WebGame

    /** The open Save dialog's warning line, updated in place while the player types. */
    private var saveWarning: Element? = null

    private val board = DomBoard(root) { game }

    private var shown: WebState? = null

    /** The line easing into place, redrawn every animation frame until it gets there. */
    private var slide: Slide? = null
    /** An animation frame is queued, so another slide or Mordie line doesn't queue a second. */
    private var framePending = false

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
    }

    override fun render(state: WebState) {
        state.announcement?.let { announcer?.say(it) }
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
        if (before != null && state.keepsBoardFrom(before)) {
            redrawAroundBoard(state, now)
            return
        }
        val dialog = state.dialog
        // A rebuild would drop the name field's focus and caret, and nothing else changes while
        // the Save dialog is open, so typing only updates the warning.
        if (dialog is WebDialog.Save && saveWarning != null) {
            saveWarning?.textContent = dialog.warning ?: ""
            drawMordie(now)
            return
        }
        saveWarning = null
        val stepsScroll = stepsScrollTop()
        val focused = focusedKey()
        root.clear()
        root.style.setProperty("--side", state.side.toString())
        root.classList.toggle("solved", state.solved)
        root.classList.toggle("dragging", state.drag != null)
        root.classList.toggle("solution", state.solution != null)
        root.classList.toggle("keyboard-mode", state.mode == PlayMode.KEYBOARD)
        root.append(
            // The box the tall layout sizes the tiles from (GH-147).
            element("div", "stage").apply {
                if (state.boardWords != null) append(readBoardButton({ shown?.boardWords }, dialog != null, announcer))
                append(board.draw(state, justSolved = state.solved && before?.solved == false, state.drag ?: slideOffset()))
                state.hints?.let { append(keyHints(it)) }
            },
            *listOfNotNull(state.solution?.let { stepList(it) }).toTypedArray(),
            panel(state),
        )
        placeSteps(state, stepsScroll)
        // Only a dialog that has just opened fades in, not one redrawn as the player picks in it.
        val reopened = dialog != null && before?.dialog?.let { it::class == dialog::class } == true
        dialog?.let { root.append(element("div", if (reopened) "overlay shown" else "overlay").apply { append(dialogElement(it, game) { warning -> saveWarning = warning }) }) }
        drawMordie(now)
        setBehindDialog(root, dialog != null)
        applyFocus(root, state.focus, focused) { fallbackFocus(it, state) }
    }

    /** A frame around the kept board (GH-171): the tiles updated, the rest redrawn, focus left or put back. */
    private fun redrawAroundBoard(state: WebState, now: Double) {
        val stepsScroll = stepsScrollTop()
        val focused = focusedKey()
        root.classList.toggle("dragging", state.drag != null)
        board.update(state, state.drag ?: slideOffset())
        state.solution?.let { root.querySelector(".steps")?.replaceWith(stepList(it)) }
        root.querySelector(".panel")?.replaceWith(panel(state))
        placeSteps(state, stepsScroll)
        drawMordie(now)
        applyFocus(root, state.focus, focused) { fallbackFocus(it, state) }
    }

    /** Beside the board when the window is wide; when tall, its items stack around it (styles.css). */
    private fun panel(state: WebState) = element("div", "panel").apply {
        val dialogOpen = state.dialog != null
        append(
            text("moves", if (state.movesShown) "Moves: ${state.moves}" else "").also { moves = it },
            element("div", "mordie").apply { append(element("div", "line")) }.also { mordie = it },
            *listOfNotNull(state.solution?.let { replayButtons(it, { game }, dialogOpen) }, giveUpRow(state.giveUp, state.digitOf(ToolbarButton.GIVE_UP_ROW), { game }, dialogOpen)).toTypedArray(),
            element("div", "toolbar").also { toolbar = it }.apply {
                append(
                    toolbarButton("New", ToolbarButton.NEW, Focus.NewButton, state) { game.onNew() },
                    toolbarButton("Save", ToolbarButton.SAVE, Focus.SaveButton, state) { game.onSave() },
                    toolbarButton("Load", ToolbarButton.LOAD, Focus.LoadButton, state) { game.onLoad() },
                    toolbarButton("Labels", ToolbarButton.LABELS, Focus.LabelsButton, state) { game.onLabels() },
                )
                // A row of its own under the four (GH-171).
                if (state.controlsShown) append(element("div", "controls-row").apply {
                    append(toolbarButton("Controls", ToolbarButton.CONTROLS, Focus.ControlsButton, state) { game.controls.open() })
                })
            },
            text("status", state.status ?: ""),
        )
    }

    private fun stepsScrollTop() = root.querySelector(".steps")?.scrollTop

    /** The redrawn step list starts where it was scrolled to, [scrollTop]: back at the top on a Restart;
     *  otherwise the current step scrolls into view, if it must. */
    private fun placeSteps(state: WebState, scrollTop: Double?) {
        val steps = root.querySelector(".steps") ?: return
        steps.scrollTop = scrollTop?.takeUnless { state.solution?.atStart == true } ?: 0.0
        (steps.querySelector(".current") as? HTMLElement)?.let { showStep(steps, it) }
    }

    override fun scrollSteps(lines: Int) {
        val steps = root.querySelector(".steps") ?: return
        val step = steps.querySelector(".step") as? HTMLElement ?: return
        steps.scrollTop += lines * step.offsetHeight
    }

    private fun toolbarButton(label: String, key: ToolbarButton, focus: Focus, state: WebState, onClick: () -> Unit) =
        button(label, onClick = onClick).focusKey(focus).withDigit(state.digitOf(key)).also { it.disabled = state.dialog != null }

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

    /** Eases the line a shift moved into place: one tile after an arrow, a key or a replay shift, the rest
     *  of the way after a drag. Any other change to the tiles, or a new drag, stops a slide under way. */
    private fun startSlide(before: WebState?, state: WebState) {
        val start = when {
            before == null || state.drag != null -> null
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
            if (state != null) board.redrawTiles(state, offset)
        }
        drawMordie(time)
        if (slide != null || feed.moving(time)) schedule()
    }

    /** The current slide's offset, before its next animation frame. */
    private fun slideOffset(): LineOffset? =
        slide?.let { Slides.at(it.start, (window.performance.now() - it.startedAt) / SLIDE_MS) }

    private companion object {
        const val SLIDE_MS = 160.0
        const val MORDIE_OUT_MS = 220.0
        const val MORDIE_IN_MS = 340.0
    }
}
