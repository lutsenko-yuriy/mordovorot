package viewmodel

import board_model.Solvability
import storage.SaveRepository

/** Read-only view of [saves] as the viewmodel shows them (GH-78): [SaveInfo]s and the
 *  "available saves" message. */
class SaveCatalog(private val saves: SaveRepository) {

    /** Every save's [SaveInfo]. Throws if the listing itself fails - unlike [listSaves], for a
     *  caller that must tell "no saves" apart from "couldn't list them". */
    fun infos(): List<SaveInfo> = saves.listSaves().map(::infoOf)

    /** [infos], or empty when the listing fails. */
    fun listSaves(): List<SaveInfo> =
        try {
            infos()
        } catch (e: Exception) {
            emptyList()
        }

    fun saveExists(name: String): Boolean =
        try {
            saves.exists(name)
        } catch (e: Exception) {
            false
        }

    fun availableSavesMessage(): String {
        // Guarded on its own - a failure here (e.g. an unreadable saves/ directory) shouldn't
        // change the load's actual result (it was still "not found"), just degrade the message.
        // No CancellationException guard needed - saves.listSaves() isn't suspend, so this
        // catch-all can never observe one; revisit if that ever changes (audit finding on PR #45).
        val names = try {
            saves.listSaves()
        } catch (e: Exception) {
            return "Could not list available saves."
        }
        return if (names.isEmpty()) {
            "No saves available."
        } else {
            "Available saves: ${names.joinToString(", ") { infoOf(it).display() }}"
        }
    }

    /** [SaveInfo] for [name] from one [SaveRepository.load]. A save that can't be read (corrupted
     *  or an IO error) gets a `null` size and no unsolvable mark - a listing degrades to showing
     *  the name alone rather than dropping the save or failing the whole listing (GH-44 WU4). No CancellationException guard needed here -
     *  same reasoning as [availableSavesMessage]'s: [SaveRepository.load] isn't suspend, so this
     *  catch-all can never observe one, even though this is reachable from suspend callers
     *  (audit finding on PR #54). */
    fun infoOf(name: String): SaveInfo =
        try {
            saves.load(name)?.let { SaveInfo(name, it.squareSide, Solvability.isSolvable(it.state)) } ?: SaveInfo(name, null)
        } catch (e: Exception) {
            SaveInfo(name, null)
        }
}
