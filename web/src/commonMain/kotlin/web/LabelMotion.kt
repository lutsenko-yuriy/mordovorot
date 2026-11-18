package web

import viewmodel.TileLabelGroup
import web.a11y.Waving

/** GH-342: the label wave and the group slide running in the Tile labels dialog, which the page's per-frame rebuild must not restart.
 *  Nothing here animates by itself: each element the page builds while one runs gets a CSS `animation-delay` of its start minus the time
 *  elapsed (negative once under way), so a rebuilt page picks up where the last one was. A new change while one runs replaces it,
 *  and its "from" is the running one's target, which completes the running one at once. [clock] is the time in ms. */
internal class LabelMotion(private val clock: () -> Double) {

    private class Wave(val to: List<String>, val tiles: List<Int>?, val old: List<String?>, val timing: LabelWave.Timing, val startedAt: Double, val id: Int)

    private class Slide(val from: TileLabelGroup, val startedAt: Double)

    private var board: Wave? = null
    private var preview: Wave? = null
    private var slide: Slide? = null
    private var ids = 0

    /** Starts or stops the motions for [state], drawn after [before]. [still] (reduced motion) starts none, so everything changes at once. */
    fun start(before: WebState?, state: WebState, still: Boolean) {
        val now = clock()
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

    /** The group that is sliding shut. */
    fun leavingGroup(): TileLabelGroup? = slide?.from

    /** How long until the running group slide is over, `null` when none runs. */
    fun slideRemainingMs(): Double? = slide?.let { LabelWave.GROUP_MS - (clock() - it.startedAt) }?.takeIf { it > 0 }

    /** The delay in ms that puts a slide's CSS animation where it is by now. */
    fun slideDelayMs(): Double = -(clock() - (slide?.startedAt ?: return 0.0))

    private fun Wave.cell(index: Int): Waving? {
        if (index !in old.indices || old[index] == to[index]) return null
        val elapsed = clock() - startedAt
        return Waving(old[index], timing.leavesAt(index) - elapsed, timing.arrivesAt(index) - elapsed, id)
    }
}
