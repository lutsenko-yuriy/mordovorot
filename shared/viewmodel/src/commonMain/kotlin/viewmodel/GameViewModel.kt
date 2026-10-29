package viewmodel

import kotlinx.coroutines.channels.ReceiveChannel

/** The platform-independent viewModel surface (GH-41, split out of GH-42's `ViewModel`) -
 *  shift (one step, or several with `shiftRow`/`shiftColumn`)/new game/save/load plus the
 *  read-only query surface (`listSaves`/`saveExists`/`currentSaveName`/`hasUnsavedProgress`/
 *  `isSolved`/`boardState`/`squareSide`) a View needs to render/re-render.
 *  Exit is not here: it is terminal-only, on the terminal's `ViewModel`. */
interface GameViewModel {

    /** The UI interactions this viewModel is waiting on, in emission order (GH-42 WU2). Exactly
     *  one consumer - the View that owns this viewModel's session, draining it in a coroutine
     *  alongside its own event loop. See [UiRequestChannel.ask]. */
    val uiRequests: ReceiveChannel<UiRequest<*>>

    /** [source] is what made the shift (GH-101, GH-110), `null` for a terminal mouse click. */
    fun shiftLeft(row: Int, source: ShiftSource? = null)
    fun shiftRight(row: Int, source: ShiftSource? = null)

    fun shiftUp(col: Int, source: ShiftSource? = null)
    fun shiftDown(col: Int, source: ShiftSource? = null)

    /** Shifts [row] by [steps] tiles, right when positive (GH-110, a drag): `|steps|` shifts, one
     *  move, with the board checked for solved only once it lands. Zero steps does nothing. */
    fun shiftRow(row: Int, steps: Int, source: ShiftSource? = null)

    /** [shiftRow] for a column, down when [steps] is positive. */
    fun shiftColumn(col: Int, steps: Int, source: ShiftSource? = null)

    /** Starts a fresh shuffled game at [size] (GH-44) - throws (matching `shiftLeft`'s
     *  "Incorrect row" contract) and tracks nothing if [size] is out of range, otherwise tracks
     *  `new_game_size_selected {size, trigger}`. [trigger] is `"startup_prompt"`, `"toolbar"`
     *  (`[ New ]` -> size-picker dialog), or `"unsolvable_save"` - see docs/ANALYTICS_EVENTS.md. */
    fun newGame(size: Int, trigger: String)

    /** Returns whether the save actually succeeded - the terminal's Exit flow needs the real
     *  outcome rather than assuming success. `suspend` (GH-42): asks the
     *  View for confirmation/messages instead of returning synchronously. */
    suspend fun saveGame(name: String): Boolean
    /** Exit chosen for an unsolvable save (GH-74) runs the [UnsolvableSaveExit] hook after the
     *  load has finished (GH-41) - whatever it throws propagates from here. */
    suspend fun loadGame(name: String)

    /** Saves with their board size (GH-44 WU4), sorted by name; empty when nothing has been
     *  saved yet, or on a storage error - lets the mouse-driven Load dialog (GH-3) show what's
     *  available without exposing storage errors to the UI layer. A save whose size couldn't be
     *  read has a `null` [SaveInfo.squareSide] rather than being dropped from the list. */
    fun listSaves(): List<SaveInfo>

    /** Whether a save named [name] already exists. Backs the mouse-driven Save dialog's
     *  overwrite warning (GH-3); degrades to `false` on a storage error, same non-throwing
     *  discipline as [listSaves]. */
    fun saveExists(name: String): Boolean

    /** The name the current game was last loaded from or saved to - the Save dialog's prefill
     *  (GH-99); `null` after a new game or at launch. */
    fun currentSaveName(): String?

    /** Whether the game has shifts no save holds yet (GH-110) - backs the web's leave-site
     *  prompt; see [UnsavedProgress]. */
    fun hasUnsavedProgress(): Boolean

    /** Whether the board is currently solved - lets the mouse-driven TUI (GH-3) show the
     *  Congratulations screen and disable the shift arrows. */
    fun isSolved(): Boolean

    /** Offers to restore a previous save at startup (0/1/2+ saves) before play begins. Public so
     *  each View can run it ahead of its own event loop (GH-3). Exit chosen for an unsolvable
     *  save (GH-74) runs the [UnsolvableSaveExit] hook last (GH-41) - whatever it throws
     *  propagates from here. */
    suspend fun restoreOnStartup()

    /** Current board tile values (0-based, GH-10 dialect) - lets a View render/re-render after
     *  every command or click. */
    fun boardState(): IntArray

    /** The board's square side (rows == columns), paired with [boardState] for layout. */
    fun squareSide(): Int

    /** Moves made this game (GH-62) - see [board_model.BoardModel.moveCount]. */
    fun moveCount(): Int

    /** What Mordie says now, without the `Mordie:` prefix (GH-83); `null` for an empty
     *  stretch or a solved board. */
    fun mordieLine(): String?

    /** Mordie's ending for the solved title (GH-83); `null` when Mordie has no result, and the
     *  view falls back to `Congratulations`. */
    fun mordieEnding(): String?

    /** The row above the toolbar (GH-126) - see [GiveUpButton]. */
    fun giveUpButton(): GiveUpButton

    /** Gives up for Mordie's solution once the player has confirmed, when [giveUpButton] is
     *  [GiveUpButton.GIVE_UP]: tracks `game_given_up` and opens the solution screen on the
     *  original arrangement, discarding the player's moves. Otherwise does nothing. */
    fun giveUp()

    /** Opens the solution screen after a win, when [giveUpButton] is
     *  [GiveUpButton.MORDIES_SOLUTION]; otherwise does nothing. */
    fun showSolution()

    /** The solution screen to draw (GH-126); `null` off it. On it, [boardState] is the replay's,
     *  [mordieLine] says how many moves Mordie made, and shifts do nothing. */
    fun solution(): SolutionState?

    /** The solution screen's Next: applies one shift and returns it, for a view to animate;
     *  `null` at the end or off the screen. */
    fun replayNext(): SolutionStep?

    /** The solution screen's Prev: undoes one shift and returns the shift that did so; `null`
     *  at the start or off the screen. */
    fun replayPrevious(): SolutionStep?

    /** The solution screen's Restart: back to the original arrangement; nothing off the screen. */
    fun replayRestart()

    /** How the current game's tiles are labelled (GH-139): its save's, else the preference's. */
    fun tileLabels(): TileLabels

    /** Tracks `screen_labels_dialog` as the Tile labels dialog opens; [openedFrom] is `toolbar`
     *  or `shortcut`. */
    fun tileLabelsDialogOpened(openedFrom: String)

    /** The Tile labels dialog's OK: relabels and remembers [labels] as the preference, tracking
     *  `tile_labels_changed`. Does nothing when [labels] is already shown. */
    fun changeTileLabels(labels: TileLabels)

    /** Tracks `screen_controls_dialog` as the Controls dialog opens (GH-170); [openedFrom] is
     *  `toolbar` or `shortcut`. */
    fun controlsDialogOpened(openedFrom: String)
}
