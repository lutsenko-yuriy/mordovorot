package presenter

interface Presenter {

    fun play()

    fun shiftLeft(row: Int)
    fun shiftRight(row: Int)

    fun shiftUp(col: Int)
    fun shiftDown(col: Int)

    fun resetGame()

    /** Returns whether the save actually succeeded - see [presenter.PresenterImpl.exitGame],
     *  which needs the real outcome rather than assuming success. */
    fun saveGame(name: String): Boolean
    fun loadGame(name: String)

    /** Ends the current session (the `exit`/`quit` command) after an optional save-first
     *  dialogue. Usually ends [play] outright; returns normally instead, leaving the session
     *  running, if the user asked to save but the save attempt failed - see
     *  [presenter.PresenterImpl.exitGame]. */
    fun exitGame()

    /** Save names, sorted; empty when nothing has been saved yet, or on a storage error -
     *  lets the mouse-driven Load dialog (GH-3) show what's available without exposing
     *  storage errors to the UI layer. */
    fun listSaves(): List<String>

    /** Whether a save named [name] already exists. Backs the mouse-driven Save dialog's
     *  overwrite warning (GH-3); degrades to `false` on a storage error, same non-throwing
     *  discipline as [listSaves]. */
    fun saveExists(name: String): Boolean

    /** Whether the board is currently solved - lets the mouse-driven TUI (GH-3) show the
     *  Congratulations screen and disable the shift arrows. */
    fun isSolved(): Boolean

    /** Offers to restore a previous save at startup (0/1/2+ saves) before play begins.
     *  Exposed (rather than private, as it started) so [view.tui.TuiView] (GH-3) can run it
     *  ahead of its own event loop, instead of [play]'s console-only loop. */
    fun restoreOnStartup()

    /** Current board tile values (0-based, GH-10 dialect) - lets the mouse-driven TUI (GH-3)
     *  render/re-render after every click, since it doesn't call [play]'s console-only loop
     *  (the only place that otherwise pushes board state out via [view.View.displayBoard]). */
    fun boardState(): IntArray

    /** The board's square side (rows == columns), paired with [boardState] for TUI layout. */
    fun squareSide(): Int
}
