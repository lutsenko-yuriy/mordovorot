package viewmodel

/** What a platform does when the player picks Exit for an unsolvable save (GH-41). Called once
 *  the shared flow has finished, so nothing shared catches what it throws. [NONE]: no Exit
 *  offered. */
fun interface UnsolvableSaveExit {
    suspend fun onExit(viewModel: GameViewModel, atStartup: Boolean)

    companion object {
        val NONE = UnsolvableSaveExit { _, _ -> }
    }
}
