package viewmodel

/** The session's solution screen (GH-126): its [replay], `null` off the screen. One per
 *  session, like [CurrentSaveName], so a mode switch keeps the screen and the replay's place. */
class SolutionScreen {
    internal var replay: SolutionReplay? = null
}
