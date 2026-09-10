package storage

import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.writeText

/** The only class that touches the filesystem. Saves live as plain-text `<name>.save` files. */
class FileSaveRepository(private val directory: Path = defaultDirectory()) : SaveRepository {

    companion object {
        private const val EXTENSION = ".save"
        fun defaultDirectory(): Path = Paths.get("saves")
    }

    override fun listSaves(): List<String> {
        if (!directory.exists()) return emptyList()
        return directory.listDirectoryEntries("*$EXTENSION")
            .map { it.name.removeSuffix(EXTENSION) }
            .sorted()
    }

    override fun exists(name: String): Boolean {
        requireSafeName(name)
        return pathFor(name).exists()
    }

    override fun save(name: String, state: IntArray, squareSide: Int) {
        requireSafeName(name)
        directory.createDirectories()
        pathFor(name).writeText("$squareSide\n${state.joinToString(" ")}\n")
    }

    override fun load(name: String): SavedBoard? {
        requireSafeName(name)
        val path = pathFor(name)
        if (!path.exists()) return null

        val lines = path.readLines()
        val squareSideLine = lines.getOrNull(0) ?: throw SaveFileFormatException(name, "missing square side")
        val valuesLine = lines.getOrNull(1) ?: throw SaveFileFormatException(name, "missing board values")

        val squareSide = squareSideLine.trim().toIntOrNull()
            ?: throw SaveFileFormatException(name, "square side is not an integer")

        val tokens = valuesLine.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val state = IntArray(tokens.size) { i ->
            tokens[i].toIntOrNull() ?: throw SaveFileFormatException(name, "board value '${tokens[i]}' is not an integer")
        }

        val expectedSize = squareSide * squareSide
        if (state.size != expectedSize) {
            throw SaveFileFormatException(name, "expected $expectedSize values, found ${state.size}")
        }
        if (state.toSet() != (0 until expectedSize).toSet()) {
            throw SaveFileFormatException(name, "values are not a permutation of 0..${expectedSize - 1}")
        }

        return SavedBoard(squareSide, state)
    }

    private fun pathFor(name: String): Path = directory.resolve("$name$EXTENSION")

    /** User input goes straight into a path - this is a real traversal guard, not ceremony. */
    private fun requireSafeName(name: String) {
        require(name.isNotBlank()) { "Save name must not be blank" }
        require(!name.contains('/') && !name.contains('\\')) { "Save name must not contain a path separator" }
        require(name.split('/', '\\').none { it == ".." }) { "Save name must not contain '..'" }
    }
}
