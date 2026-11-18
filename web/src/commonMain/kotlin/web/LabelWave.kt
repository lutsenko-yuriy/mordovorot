package web

import viewmodel.TileLabelGroup

/** GH-342, GH-343: the timing of the label wave, and the frame rules that start it and the group slide.
 *  A wave runs through the tiles in reading order: tile i's old label leaves over [MOTION_MS] from `i * step`, and its new
 *  label arrives from `(i + 1) * step`, as tile i + 1's old label starts to leave. */
object LabelWave {
    /** How long the whole wave takes on the board, whatever its size. */
    const val TOTAL_MS = 600.0

    /** How long one label takes to fade and shift, leaving or arriving. */
    const val MOTION_MS = 150.0

    /** The gap between the preview strip's tiles. */
    const val PREVIEW_STEP_MS = 40.0

    /** How long a group's body takes to slide open or shut. */
    const val GROUP_MS = 250.0

    /** The board's wave over [count] tiles: the gap between tiles shrinks as the board grows, so the last label is in at [TOTAL_MS]. */
    fun board(count: Int) = Timing(count, (TOTAL_MS - MOTION_MS) / count)

    /** The preview strip's wave over [count] tiles. */
    fun preview(count: Int) = Timing(count, PREVIEW_STEP_MS)

    /** A wave over [count] tiles, [stepMs] apart. */
    class Timing(val count: Int, val stepMs: Double) {
        /** When tile [i]'s old label starts to leave, from the wave's start. */
        fun leavesAt(i: Int): Double = i * stepMs

        /** When tile [i]'s new label starts to arrive. */
        fun arrivesAt(i: Int): Double = (i + 1) * stepMs

        /** When the last new label is in. */
        val endMs: Double get() = count * stepMs + MOTION_MS

        fun over(elapsedMs: Double): Boolean = elapsedMs >= endMs
    }

    fun slideOver(elapsedMs: Double): Boolean = elapsedMs >= GROUP_MS
}

/** The board's old texts when this frame starts a label wave from [before]: the scheme of the labels changed, whatever changed it (GH-343).
 *  A row picked, Cancel or Escape, a share link's prompt opening or No, a new game or a load do; OK and Yes don't (the board already shows the labels).
 *  Each text is `null` when there is no earlier board to leave, a page still [opening] or a board of another size: the labels only arrive.
 *  `null` when nothing starts, also for a drag under way. */
internal fun WebState.labelWaveFrom(before: WebState): List<String?>? {
    if (before.labels == labels || drag != null) return null
    val old: List<String?> = if (opening || before.side != side) List(tiles.size) { null } else before.tileTexts
    return old.takeIf { olds -> olds.indices.any { olds[it] != tileTexts[it] } }
}

/** The preview strip's old texts when this frame starts its wave: the dialog stays open and the marked scheme's first labels change. */
internal fun WebState.previewWaveFrom(before: WebState): List<String>? {
    val now = dialog as? WebDialog.Labels ?: return null
    val old = (before.dialog as? WebDialog.Labels)?.preview ?: return null
    return old.takeIf { it != now.preview }
}

/** The group that was open when this frame opens another, for the slide. */
internal fun WebState.labelGroupSlideFrom(before: WebState): TileLabelGroup? {
    val now = dialog as? WebDialog.Labels ?: return null
    val old = before.dialog as? WebDialog.Labels ?: return null
    return old.open.takeIf { it != now.open }
}
