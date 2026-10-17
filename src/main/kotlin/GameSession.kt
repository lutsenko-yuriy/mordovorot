import mordie.Mordie
import view.View
import viewmodel.ModeSwitchRequestedException
import viewmodel.catchingNonControl

/** The session loop extracted out of [main] for GH-30: rebuilds the `View` per [InputMode] on
 *  [ModeSwitchRequestedException] and stops when `play()` returns normally. [buildGameSession]
 *  wires [buildView] so every rebuild shares one board and saves. Stops [mordie]'s search
 *  (GH-83) when the session ends. */
class GameSession(
    initialMode: InputMode,
    private val mordie: Mordie,
    /** Builds the `View` for a mode; `startupRestoreDone` is `true` on every build after the first. */
    private val buildView: (mode: InputMode, startupRestoreDone: Boolean) -> View,
) {

    private var mode: InputMode = initialMode

    /** `true` after the first `View` build, so a mode switch never re-shows the startup restore
     *  prompt. */
    private var startupRestoreDone = false

    suspend fun run() {
        try {
            loop()
        } finally {
            mordie.stop()
        }
    }

    private suspend fun loop() {
        var isFirstSession = true
        // The mode to fall back to if the current build fails - null when there's none: the
        // first session, or a build that is itself already a fallback.
        var fallbackMode: InputMode? = null
        // Shown by the next View rather than printed - the full-screen TUI would cover stderr.
        var notice: String? = null
        while (true) {
            // Other control-flow exceptions must reach main (PR #37, PR #43).
            try {
                catchingNonControl(
                    onError = { e ->
                        // Failed rebuild (e.g. no `stty`): go back to the previous mode to keep
                        // the game (GH-79 D1), unless this is startup or the fallback failed too.
                        val previous = fallbackMode
                        if (isFirstSession || previous == null) throw e
                        notice = "Could not switch to ${mode.name.lowercase()} mode (${e.message ?: e::class.simpleName}) " +
                            "- staying in ${previous.name.lowercase()} mode."
                        mode = previous
                        fallbackMode = null
                        startupRestoreDone = true
                    },
                ) {
                    // buildView is guarded too, so a rebuild failure falls back like a play()
                    // failure (PR #37).
                    val view = buildView(mode, startupRestoreDone)
                    view.play(notice.also { notice = null })
                    return
                }
            } catch (e: ModeSwitchRequestedException) {
                fallbackMode = mode
                mode = e.target
                startupRestoreDone = true
            }
            isFirstSession = false
        }
    }
}
