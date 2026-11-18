package web

import kotlinx.browser.window
import org.w3c.dom.Element
import viewmodel.TileLabelGroup
import web.a11y.Waving
import web.a11y.hiddenFromReaders

/** GH-342: the label wave and the group slide running in the Tile labels dialog, which the page's per-frame rebuild must not restart.
 *  Nothing here animates by itself: each element built while one runs gets a CSS `animation-delay` of its start minus the time
 *  elapsed (negative once under way), so a rebuilt page picks up where the last one was. A new change while one runs replaces it,
 *  and its "from" is the running one's target, which completes the running one at once. */
internal class LabelMotion {

    private class Wave(val to: List<String>, val tiles: List<Int>?, val old: List<String>, val timing: LabelWave.Timing, val startedAt: Double, val id: Int)

    private class Slide(val from: TileLabelGroup, val startedAt: Double)

    private var board: Wave? = null
    private var preview: Wave? = null
    private var slide: Slide? = null
    private var ids = 0

    /** Starts or stops the motions for [state], drawn after [before]. [still] (reduced motion) starts none, so everything changes at once. */
    fun start(before: WebState?, state: WebState, still: Boolean) {
        val now = window.performance.now()
        if (still) {
            board = null
            preview = null
            slide = null
            return
        }
        val boardFrom = before?.let(state::labelWaveFrom)
        board = if (boardFrom != null) Wave(state.tileTexts, state.tiles, boardFrom, LabelWave.board(state.tiles.size), now, ++ids)
        else board?.takeIf { !it.timing.over(now - it.startedAt) && it.to == state.tileTexts && it.tiles == state.tiles && state.drag == null }

        val labels = state.dialog as? WebDialog.Labels
        val previewFrom = before?.let(state::previewWaveFrom)
        preview = if (previewFrom != null && labels != null) Wave(labels.preview, null, previewFrom, LabelWave.preview(labels.preview.size), now, ++ids)
        else preview?.takeIf { labels != null && !it.timing.over(now - it.startedAt) && it.to == labels.preview }

        val slideFrom = before?.let(state::labelGroupSlideFrom)
        slide = if (slideFrom != null) Slide(slideFrom, now)
        else slide?.takeIf { labels != null && !LabelWave.slideOver(now - it.startedAt) }
    }

    /** What the board's tile at [index] does now: its old label leaving and its new one arriving; `null` for a tile that reads the same. */
    fun tile(index: Int): Waving? = board?.cell(index)

    /** The same for the preview strip's swatch at [index]. */
    fun swatch(index: Int): Waving? = preview?.cell(index)

    /** The group that is sliding shut, and the delay in ms that puts its CSS animation where it is by now. */
    fun leavingGroup(): TileLabelGroup? = slide?.from

    fun slideDelayMs(): Double = -(window.performance.now() - (slide?.startedAt ?: return 0.0))

    private fun Wave.cell(index: Int): Waving? {
        if (index !in old.indices || old[index] == to[index]) return null
        val elapsed = window.performance.now() - startedAt
        return Waving(old[index], timing.leavesAt(index) - elapsed, timing.arrivesAt(index) - elapsed, id)
    }
}

/** The leaving group's rows as a copy that only looks like them: nothing in it can be focused, pressed or read. */
internal fun leavingBody(group: TileLabelGroup, dialog: WebDialog.Labels, delayMs: Double): Element =
    hiddenFromReaders(element("div", "group-body leaving")).apply {
        setAttribute("inert", "")
        setAttribute("style", "animation-delay: ${delayMs}ms")
        append(
            element("div", "group-inner").apply {
                setAttribute("style", "animation-delay: ${delayMs}ms")
                append(
                    element("div", "choices list labels").apply {
                        group.schemes.mapNotNull { scheme -> dialog.options.firstOrNull { it.labels == scheme } }.forEach { option ->
                            append(copyOfRow(option, option.labels == dialog.selected))
                        }
                    },
                )
            },
        )
    }

/** The open group's rows with the entering slide on them. */
internal fun enteringBody(choices: Element, delayMs: Double): Element =
    element("div", "group-body entering").apply {
        setAttribute("style", "animation-delay: ${delayMs}ms")
        append(
            element("div", "group-inner").apply {
                setAttribute("style", "animation-delay: ${delayMs}ms")
                append(choices)
            },
        )
    }

private fun copyOfRow(option: LabelOption, checked: Boolean): Element =
    element("button", if (checked) "checked" else null).apply {
        setAttribute("tabindex", "-1")
        append(
            element("span", "name").apply { textContent = option.title },
            *listOfNotNull(option.limit?.let { element("span", "limit").apply { textContent = it } }).toTypedArray(),
        )
    }
