package web

import viewmodel.BoardKey
import viewmodel.Direction
import viewmodel.GameViewModel
import viewmodel.GiveUpButton
import viewmodel.KeyShift
import viewmodel.SolutionKey
import viewmodel.TileSelection

/** The keys outside dialogs and the selected tile (GH-171). */
internal class WebKeyPlay(
    private val game: WebGame,
    private val viewModel: GameViewModel,
    private val pressedWith: PressedWith,
    private val controlsShown: Boolean,
) {

    /** Kept across a mode switch, a new game and a load, clamped to a smaller board. */
    private var selection = TileSelection.INITIAL

    private val keyboardMode get() = game.controls.mode == PlayMode.KEYBOARD

    /** A dialog a digit opens gives focus back to the control keyed [from]. */
    fun onKey(press: KeyPress, from: String?): Boolean {
        pressedWith.key()
        if (!game.idle) return false
        val boardFocused = from == Focus.Board.key
        val onSolution = boardFocused && viewModel.solution() != null
        when (val key = WebKeys.pageKey(press, game.controls.mode, controlsShown, game.controls.layout, boardFocused && game.playable, onSolution)) {
            is PageKey.Digit -> onDigit(key.button, from)
            is PageKey.Board -> onBoardKey(key.key)
            is PageKey.Solution -> onSolutionKey(key.key)
            PageKey.ReadBoard -> game.readBoard()
            null -> return false
        }
        return true
    }

    /** In mouse mode a press on a tile only starts a drag; on the solution screen there's no selection to move. */
    fun onTilePressed(row: Int, col: Int) {
        if (!keyboardMode || !game.idle) return
        if (game.playable) selection = TileSelection(row, col) else if (viewModel.solution() == null) return
        game.render(focus = Focus.Board)
    }

    /** The selection this frame shows: in keyboard mode, during play. */
    fun shownSelection(): TileSelection? {
        selection = selection.clampedTo(viewModel.squareSide())
        return selection.takeIf { keyboardMode && !viewModel.isSolved() && viewModel.solution() == null }
    }

    /** A shift's [words], with the tile now under the selection in keyboard mode. */
    fun withTileNow(words: String): String {
        val at = shownSelection() ?: return words
        val side = viewModel.squareSide()
        return Announcements.withNow(words, viewModel.tileLabels().label(viewModel.boardState()[at.row * side + at.col], side))
    }

    private fun onDigit(button: ToolbarButton, from: String?) {
        val opener = when (from) {
            null -> Focus.PageStart
            Focus.Board.key -> Focus.Board
            else -> Focus.Restore(from)
        }
        when (button) {
            ToolbarButton.NEW -> game.onNew(opener)
            ToolbarButton.SAVE -> game.onSave(opener)
            ToolbarButton.LOAD -> game.onLoad(opener)
            ToolbarButton.LABELS -> game.onLabels("shortcut", opener)
            ToolbarButton.GIVE_UP_ROW -> when (viewModel.giveUpButton()) {
                GiveUpButton.GIVE_UP -> game.solution.onGiveUp(opener)
                GiveUpButton.MORDIES_SOLUTION -> game.solution.onMordiesSolution()
                GiveUpButton.DIMMED, GiveUpButton.HIDDEN -> {}
            }
            ToolbarButton.CONTROLS -> game.controls.open("shortcut", opener)
        }
    }

    private fun onSolutionKey(key: SolutionKey) = when (key) {
        SolutionKey.PREV -> game.solution.onReplayPrevious()
        SolutionKey.NEXT -> game.solution.onReplayNext()
        SolutionKey.RESTART -> game.solution.onReplayRestart()
        SolutionKey.SCROLL_UP -> game.scrollSteps(-1)
        SolutionKey.SCROLL_DOWN -> game.scrollSteps(1)
    }

    private fun onBoardKey(key: BoardKey) = when (key) {
        is BoardKey.Select -> {
            selection = selection.move(key.direction, viewModel.squareSide())
            game.render()
        }
        is BoardKey.Shift -> when (key.direction) {
            Direction.LEFT -> game.shiftLine(Arrow.LEFT, selection.row, KeyShift(key.key))
            Direction.RIGHT -> game.shiftLine(Arrow.RIGHT, selection.row, KeyShift(key.key))
            Direction.UP -> game.shiftLine(Arrow.UP, selection.col, KeyShift(key.key))
            Direction.DOWN -> game.shiftLine(Arrow.DOWN, selection.col, KeyShift(key.key))
        }
    }
}
