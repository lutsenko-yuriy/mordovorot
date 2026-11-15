package viewmodel

/** The session's solution screen (GH-126): its [replay], `null` off the screen. One per
 *  session, like [CurrentSaveName], so a mode switch keeps the screen and the replay's place. */
class SolutionScreen {
    internal var replay: SolutionReplay? = null

    /** How the player's game ended before the screen (GH-273); `null` for a save from before it. Set and
     *  cleared with [replay]. */
    internal var result: PlayerResult? = null
}
