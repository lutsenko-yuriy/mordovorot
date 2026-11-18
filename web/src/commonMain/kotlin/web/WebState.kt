package web

import texts.Language
import texts.texts
import viewmodel.GiveUpButton
import viewmodel.KeyboardLayout
import viewmodel.SharedProgress
import viewmodel.SolutionState
import viewmodel.TileLabelGroup
import viewmodel.TileLabels
import viewmodel.TileSelection
import web.a11y.Announcements
import web.drag.LineOffset
import web.qr.QrCode

/** One frame of the page (GH-110): what [WebRenderer] draws. [tiles] are 1-based, row by row. */
data class WebState(
    /** The language the page is worded in (GH-275). */
    val language: Language = Language.EN,
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
    /** The main input isn't touch (GH-287): the keys, digits, hints and `[ Controls 6 ]` show, hidden on touch screens. */
    val computer: Boolean = false,
    /** The selected tile, on a computer during play (GH-171). */
    val selection: TileSelection? = null,
    /** Whether the outlines on [selection] show (GH-287): after a select or shift key, until a press on a tile or an arrow. The selection drives the grid and the Tab stop either way. */
    val selectionShown: Boolean = false,
    /** The key hints under the board, or the solution screen's keys, on a computer (GH-171). */
    val hints: List<KeyHint>? = null,
    /** The footer line, with the New version available notice once one is ready (GH-262). */
    val footer: List<FooterPart> = emptyList(),
    /** The tutorial's panel (GH-288), in place of the Give Up row and the toolbar; `null` off the guided steps. */
    val guidance: Guidance? = null,
    /** `[ Tutorial ]` shows in the Give Up row (GH-289): the tutorial can be replayed. */
    val tutorialButton: Boolean = false,
    /** `[ Hint ]` in the Give Up row (GH-332): hidden on the solution screen and in the tutorial, dimmed for a scheme with no letters to show. */
    val alphabetHint: AlphabetHintButton = AlphabetHintButton.HIDDEN,
    /** The page is still opening (GH-343): drawn before the startup is over, so a change of labels has no earlier board to leave. */
    val opening: Boolean = false,
) {
    /** The digit [button] shows, `null` when it shows none (GH-171). */
    fun digitOf(button: ToolbarButton): Int? = button.digit.takeIf { WebKeys.live(button, computer) }

    /** A computer's board is a Tab stop, during play and on the solution screen (GH-171). */
    val boardFocusable: Boolean get() = selection != null || boardName != null

    /** What a reader calls the solution screen's board on taking focus (GH-171); `null` off it and on touch. */
    val boardName: String? get() = solution?.takeIf { computer }?.let { Announcements.boardStep(it.moves, it.current) }

    /** Whether the page keeps the board's element and updates it in place: rebuilt and refocused, it
     *  has VoiceOver say the board over the live region, or keep saying the tile just left (GH-171). A new language
     *  rebuilds it, so its `Board` name and Read board text are reworded (GH-275), and so do other labels, so
     *  the board's class for Cyrillic pairs follows them (GH-313: a link or a load can change them with no dialog). */
    fun keepsBoardFrom(before: WebState): Boolean =
        language == before.language && labels == before.labels && boardFocusable && before.boardFocusable && dialog == null && before.dialog == null &&
            side == before.side && solved == before.solved && (solution == null) == (before.solution == null) &&
            (guidance == null) == (before.guidance == null)

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
    /** The new-game size picker, with Cancel; [current] is the board's side. [veryLarge] are the
     *  sizes offered in their own warned section (GH-207). */
    data class Size(val sizes: List<Int>, val current: Int, val veryLarge: List<Int> = emptyList()) : WebDialog

    /** The name being typed, with the overwrite warning when a save by that name exists. */
    data class Save(val name: String, val warning: String?) : WebDialog

    /** The toolbar's Load, or with [restore] the startup offer to restore one of [saves]. [focus] is
     *  where it opens when not on the first save, and [message] a line above the list: both set when
     *  it is shown again after a delete (GH-251). */
    data class Load(val restore: Boolean, val saves: List<SaveEntry>, val focus: Focus? = null, val message: String? = null) : WebDialog

    /** The confirmation before deleting the save [name] (GH-251): Cancel, where focus starts, or Delete. */
    data class DeleteSave(val name: String) : WebDialog

    /** A save no sequence of shifts solves: Repair & load or New game, and no Exit. */
    data class Unsolvable(val message: String) : WebDialog

    /** The give-up confirmation (GH-126): Yes or No. */
    data object GiveUp : WebDialog

    /** The tile labels picker (GH-139): [selected] is marked until Cancel or OK. [message] says why
     *  a load opened it (GH-160). Only the [open] group lists its schemes (GH-323); the mark can be in another. */
    data class Labels(
        val options: List<LabelOption>,
        val selected: TileLabels,
        val message: String? = null,
        val open: TileLabelGroup = TileLabelGroup.of(selected),
        /** The row that doesn't fit the board and was pressed or reached by an arrow (GH-343): its limit shows over it. Never marked. */
        val tip: TileLabels? = null,
    ) : WebDialog {
        /** The preview strip: the marked scheme's first labels, the same on every board (GH-323). */
        val preview: List<String> get() = selected.sampleRow(PREVIEW_LENGTH)

        /** A group header's name for screen readers: the group's title, and for a closed group that holds the mark, which scheme
         *  is marked too, since the preview strip is hidden from readers and a closed group lists no rows. */
        fun headerName(group: TileLabelGroup): String =
            if (group != open && TileLabelGroup.of(selected) == group) texts().page.labelGroupMarked(group.title, group.rowTitle(selected)) else group.title

        /** The [open] group's options, in order. */
        val openOptions: List<LabelOption> get() = open.schemes.mapNotNull { scheme -> options.firstOrNull { it.labels == scheme } }
    }

    /** The Controls dialog (GH-171): [layout] is marked until Cancel or OK; [keyList]
     *  names the marked layout's keys. */
    data class Controls(val layout: KeyboardLayout, val keyList: List<String>) : WebDialog

    /** The Share dialog (GH-243): the [link] to a [side] x [side] board - the one Mordie started from when
     *  [startKnown], else the board as it is; [qr] is the link as a code, `null` when too long for one. */
    data class Share(
        val link: String,
        val side: Int,
        val startKnown: Boolean,
        val qr: QrCode?,
        val copied: Boolean = false,
        /** The scheme shown on the board, which the link carries when [includeLabels] is on (GH-267). */
        val labels: TileLabels = TileLabels.DEFAULT,
        val includeLabels: Boolean = false,
        /** What the sender's game says now, which the link carries when [includeProgress] is on; `null` at 0 moves (GH-272). */
        val progress: SharedProgress? = null,
        val includeProgress: Boolean = false,
    ) : WebDialog {
        /** The toggle's text, which also names it for readers. */
        val labelsToggle: String get() = texts().dialogs.shareLabels(labels.title)

        /** The progress toggle's text: what it would send. */
        val progressToggle: String get() = with(texts().dialogs) {
            when (progress) {
                null -> shareProgress
                is SharedProgress.Solving -> shareProgressSolving(progress.moves)
                is SharedProgress.Solved -> shareProgressSolved(progress.moves)
                is SharedProgress.GaveUp -> progress.moves.let { if (it == null) shareProgressGaveUp else shareProgressGaveUpAfter(it) }
            }
        }

        /** The progress toggle can be flipped: there is progress to send. */
        val progressAvailable: Boolean get() = progress != null

        val note: String get() = with(texts().dialogs) { if (startKnown) shareNoteSame(side) else shareNoteNow(side) }
    }

    /** A share link's board can't be opened (GH-243): [message] says why; OK. */
    data class LinkRejected(val message: String) : WebDialog

    /** A share link's [side] x [side] board would replace a game with unsaved moves (GH-243): Cancel or Open. */
    data class OpenShared(val side: Int) : WebDialog

    /** A share link's tile labels differ from the player's own (GH-267): Yes shows the sender's [labels] for this
     *  game only, No keeps the player's. [labels] are previewed behind the dialog. */
    data class SharedLabels(val labels: TileLabels) : WebDialog {
        val message: String get() = texts().dialogs.sharedLabelsMessage
    }

    /** The question before the tutorial replaces a game with unsaved progress (GH-289): Save, Don't save or Cancel, where focus starts. */
    data object ReplaySave : WebDialog {
        val message: String get() = texts().dialogs.replaySaveMessage
    }

    /** The confirmation before updating with unsaved progress (GH-262): Cancel, where focus starts, or Update. */
    data object Update : WebDialog

    /** The alphabet hint (GH-332): [hint]'s whole alphabet, the letters in use outlined and the rest greyed; OK. */
    data class AlphabetHint(val hint: viewmodel.AlphabetHint) : WebDialog {
        /** The alphabet's name, which titles the dialog. */
        val alphabetTitle: String get() = with(texts().dialogs) {
            when (hint.group) {
                TileLabelGroup.CYRILLIC -> alphabetCyrillic
                TileLabelGroup.GREEK -> alphabetGreek
                else -> alphabetLatin
            }
        }

        /** Whether the letter at [index] is in use on the board. */
        fun inUse(index: Int): Boolean = index < hint.used

        /** The letter at [index] as a reader says it: as Read board does, and a greyed one marked as not on the board. */
        fun letterName(index: Int): String =
            Announcements.spoken(hint.letters[index]).let { if (inUse(index)) it else texts().reader.notOnBoard(it) }
    }
}

/** `[ Hint ]`'s state (GH-332): [DIMMED] is shown but does nothing. */
enum class AlphabetHintButton { HIDDEN, DIMMED, LIVE }

/** A save as a Load row: its [label] is `name (NxN)`, with the unsolvable mark when it applies. */
data class SaveEntry(val name: String, val label: String) {
    /** What its trash button is called (GH-251). */
    val deleteName: String get() = texts().dialogs.deleteName(name)
}

/** How many labels the Labels dialog's preview strip shows (GH-323). */
const val PREVIEW_LENGTH = 3

/** A Labels row: a scheme and its [title]; when it doesn't fit the board (GH-160) [limit] says the
 *  largest side it labels, and the row is not [available]. */
data class LabelOption(val labels: TileLabels, val title: String, val limit: String? = null) {
    val available: Boolean get() = limit == null
}
