package web

import viewmodel.GiveUpButton
import viewmodel.KeyboardLayout
import viewmodel.SolutionState
import viewmodel.TileLabels
import viewmodel.TileSelection
import web.a11y.Announcements
import web.a11y.Focus
import web.drag.LineOffset

/** One frame of the page (GH-110): what [WebRenderer] draws. [tiles] are 1-based, row by row. */
data class WebState(
    val side: Int,
    val tiles: List<Int>,
    val moves: Int,
    /** Mordie's line while playing, its ending (or `Congratulations ✓`) once solved; `null` when silent. */
    val mordie: String?,
    val solved: Boolean,
    /** The last status message (D4), cleared by the next action. */
    val status: String?,
    val dialog: WebDialog?,
    /** The line being dragged and how far, before the release snaps it. */
    val drag: LineOffset? = null,
    /** The row above the toolbar (GH-126). */
    val giveUp: GiveUpButton = GiveUpButton.HIDDEN,
    /** Mordie's solution screen (GH-126); `null` off it. */
    val solution: SolutionState? = null,
    /** The replay shift just made, for this frame to slide its line from. */
    val slide: LineOffset? = null,
    /** How the tiles are labelled (GH-139). */
    val labels: TileLabels = TileLabels.DEFAULT,
    /** The control to focus once this frame is drawn (GH-163); `null` leaves focus alone. */
    val focus: Focus? = null,
    /** What a screen reader says once this frame is drawn (GH-166); `null` says nothing. One-time
     *  like [focus], so the same text on two frames is said twice. */
    val announcement: String? = null,
    val mode: PlayMode = PlayMode.DEFAULT,
    /** The `[ Controls 6 ]` row (GH-171), hidden on touch screens. */
    val controlsShown: Boolean = false,
    /** The selected tile, in keyboard mode during play (GH-171). */
    val selection: TileSelection? = null,
    /** The key hints under the board, or the solution screen's keys, in keyboard mode (GH-171). */
    val hints: List<KeyHint>? = null,
) {
    /** The digit [button] shows, `null` when it shows none (GH-171). */
    fun digitOf(button: ToolbarButton): Int? = button.digit.takeIf { WebKeys.live(button, mode, controlsShown) }

    /** Keyboard mode's board is a Tab stop, during play and on the solution screen (GH-171). */
    val boardFocusable: Boolean get() = selection != null || boardName != null

    /** What a reader calls the solution screen's board on taking focus (GH-171); `null` off it and in mouse mode. */
    val boardName: String? get() = solution?.takeIf { mode == PlayMode.KEYBOARD }?.let { Announcements.boardStep(it.moves, it.current) }

    /** Whether the page keeps the board's element and updates it in place: rebuilt and refocused, it
     *  has VoiceOver say the board over the live region, or keep saying the tile just left (GH-171). */
    fun keepsBoardFrom(before: WebState): Boolean =
        boardFocusable && before.boardFocusable && dialog == null && before.dialog == null &&
            side == before.side && solved == before.solved && (solution == null) == (before.solution == null)

    /** Each tile's text, in [tiles]' order. */
    val tileTexts: List<String> get() = tiles.map { labels.label(it - 1, side) }

    /** During play, whether each tile, in [tiles]' order, is home (GH-167); empty off play. */
    val home: List<Boolean> get() = if (arrowsEnabled) tiles.mapIndexed { i, value -> value == i + 1 } else emptyList()

    /** Read board's words during play (GH-167), `null` off play. */
    val boardWords: String? get() = if (arrowsEnabled) Announcements.board(tileTexts, home, side) else null

    val arrowsEnabled: Boolean get() = !solved && solution == null
    val movesShown: Boolean get() = solution == null
}

/** The modal dialog open over the board. */
sealed interface WebDialog {
    /** The new-game size picker, with Cancel; [current] is the board's side. */
    data class Size(val sizes: List<Int>, val current: Int) : WebDialog

    /** The name being typed, with the overwrite warning when a save by that name exists. */
    data class Save(val name: String, val warning: String?) : WebDialog

    /** The toolbar's Load, or with [restore] the startup offer to restore one of [saves]. */
    data class Load(val restore: Boolean, val saves: List<SaveEntry>) : WebDialog

    /** A save no sequence of shifts solves: Repair & load or New game, and no Exit. */
    data class Unsolvable(val message: String) : WebDialog

    /** The give-up confirmation (GH-126): Yes or No. */
    data object GiveUp : WebDialog

    /** The tile labels picker (GH-139): [selected] is marked until Cancel or OK. [message] says why
     *  a load opened it (GH-160). */
    data class Labels(val options: List<LabelOption>, val selected: TileLabels, val message: String? = null) : WebDialog

    /** The Controls dialog (GH-171): [mode] and [layout] are marked until Cancel or OK; [keyList]
     *  names the marked layout's keys. */
    data class Controls(val mode: PlayMode, val layout: KeyboardLayout, val keyList: List<String>) : WebDialog
}

/** A save as a Load row: its [label] is `name (NxN)`, with the unsolvable mark when it applies. */
data class SaveEntry(val name: String, val label: String)

/** A Labels row: a scheme, its [title] and its [sample], the solved board's first row - or, when
 *  not [available] for the board (GH-160), the largest side it labels. */
data class LabelOption(val labels: TileLabels, val title: String, val sample: List<String>, val available: Boolean = true)
