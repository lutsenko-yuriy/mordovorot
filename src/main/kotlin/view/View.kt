package view

/**
 * Created by yurich on 08.12.16.
 */
interface View {
    fun displayBoard(boardState: IntArray, squareSide: Int)

    /** `suspend` (GH-42): this and the four prompts below are the presenter-facing interaction
     *  surface that will move off blocking calls onto a request channel in a later WU - marking
     *  them `suspend` now keeps that migration from being a call-signature change later. */
    suspend fun showMessage(message: String)

    suspend fun processCommand()

    /** Prompts the user to restore the single save [saveName] found at startup.
     *  `false` on a "no" answer or EOF. */
    suspend fun confirmRestore(saveName: String): Boolean

    /** Prompts the user to pick one of [saveNames] to restore at startup, or start a new
     *  game. Returns the raw typed name (which may not be one of [saveNames] - the caller
     *  re-prompts on an unknown name), or `null` on a blank answer or EOF. */
    suspend fun chooseSaveToRestore(saveNames: List<String>): String?

    /** Prompts "Save before quitting?" when the user runs `exit`/`quit`. `false` on a "no"
     *  answer or EOF - same contract as [confirmRestore]. */
    suspend fun confirmSaveBeforeExit(): Boolean

    /** Prompts for a save name after [confirmSaveBeforeExit] returns `true`. Returns the raw
     *  typed name, or `null` on a blank answer or EOF (the caller treats that as "don't save"
     *  rather than re-prompting). */
    suspend fun promptSaveName(): String?

    suspend fun play()
}