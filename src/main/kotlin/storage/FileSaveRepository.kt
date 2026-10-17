package storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

/** The only class that touches the filesystem. Saves live as plain-text `<name>.save` files,
 *  in [SaveFileCodec]'s format. */
class FileSaveRepository(private val directory: Path = defaultDirectory()) : SaveRepository {

    companion object {
        private const val EXTENSION = ".save"
        fun defaultDirectory(): Path = Paths.get("saves")
    }

    override fun listSaves(): List<String> {
        if (!directory.exists()) return emptyList()
        return directory.listDirectoryEntries("*$EXTENSION")
            .map { it.name.removeSuffix(EXTENSION) }
            .filter { SaveNames.isSafe(it) }
            .sorted()
    }

    override fun exists(name: String): Boolean {
        SaveNames.requireSafe(name)
        return pathFor(name).exists()
    }

    override fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int, mordie: SavedMordie?) {
        SaveNames.requireSafe(name)
        val text = SaveFileCodec.encode(squareSide, state, moveCount, mordie)

        directory.createDirectories()
        // Write to a temp file and move atomically so a crash or a rejected write
        // mid-flight can't truncate an existing save (findings from WU2's audit).
        val temp = Files.createTempFile(directory, "$name-", ".tmp")
        try {
            temp.writeText(text)
            Files.move(temp, pathFor(name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.deleteIfExists(temp)
            throw e
        }
    }

    override fun load(name: String): SavedBoard? {
        SaveNames.requireSafe(name)
        val path = pathFor(name)
        if (!path.exists()) return null
        return SaveFileCodec.decode(name, Files.readString(path))
    }

    override fun isValidName(name: String): Boolean = SaveNames.isSafe(name)

    private fun pathFor(name: String): Path = directory.resolve("$name$EXTENSION")
}
