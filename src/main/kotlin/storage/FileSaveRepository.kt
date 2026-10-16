package storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
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
        private const val FOUND = "found"
        private const val SEARCHING = "searching"
        fun defaultDirectory(): Path = Paths.get("saves")
    }

    override fun listSaves(): List<String> {
        if (!directory.exists()) return emptyList()
        return directory.listDirectoryEntries("*$EXTENSION")
            .map { it.name.removeSuffix(EXTENSION) }
            .filter { isSafeName(it) }
            .sorted()
    }

    override fun exists(name: String): Boolean {
        requireSafeName(name)
        return pathFor(name).exists()
    }

    override fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int, mordie: SavedMordie?) {
        requireSafeName(name)
        require(squareSide > 0) { "Square side must be positive" }
        require(state.size == squareSide * squareSide) { "Expected ${squareSide * squareSide} values, got ${state.size}" }
        require(isArrangement(state, state.size)) { "State must contain each of ${state.size} tile values exactly once" }
        require(moveCount >= 0) { "Move count must not be negative" }
        if (mordie != null) {
            require(isArrangement(mordie.original, state.size)) { "Mordie's original must be a valid ${state.size}-tile arrangement" }
            require(mordie.moves >= 0) { "Mordie's moves must not be negative" }
        }

        directory.createDirectories()
        // Write to a temp file and move atomically so a crash or a rejected write
        // mid-flight can't truncate an existing save (findings from WU2's audit).
        val temp = Files.createTempFile(directory, "$name-", ".tmp")
        try {
            val mordieLines = mordie?.let { "${it.original.joinToString(" ")}\n${it.moves} ${if (it.found) FOUND else SEARCHING}\n" }
            temp.writeText("$squareSide\n${state.joinToString(" ")}\n$moveCount\n${mordieLines.orEmpty()}")
            Files.move(temp, pathFor(name), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.deleteIfExists(temp)
            throw e
        }
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
        if (squareSide <= 0) throw SaveFileFormatException(name, "square side must be positive")

        val expectedSize = squareSide * squareSide
        val state = parseArrangement(name, valuesLine, expectedSize, "board")

        // Saves from before GH-62 have no third line - they load with no moves made.
        val moveCountLine = lines.getOrNull(2)?.trim().orEmpty()
        val moveCount = if (moveCountLine.isEmpty()) 0 else moveCountLine.toIntOrNull()
            ?: throw SaveFileFormatException(name, "move count is not an integer")
        if (moveCount < 0) throw SaveFileFormatException(name, "move count must not be negative")

        return SavedBoard(squareSide, state, moveCount, parseMordie(name, lines, expectedSize))
    }

    /** Lines 4-5 (GH-83): the original arrangement, then `<N> found` or `<N> searching`.
     *  A missing or blank line 4 means no Mordie data. */
    private fun parseMordie(name: String, lines: List<String>, expectedSize: Int): SavedMordie? {
        val originalLine = lines.getOrNull(3)?.takeIf { it.isNotBlank() } ?: return null
        val original = parseArrangement(name, originalLine, expectedSize, "Mordie's original")
        val status = lines.getOrNull(4)?.trim()?.split(Regex("\\s+"))
            ?: throw SaveFileFormatException(name, "missing Mordie's status")
        val moves = status.getOrNull(0)?.toIntOrNull()
        val keyword = status.getOrNull(1)
        if (status.size != 2 || moves == null || moves < 0 || keyword !in setOf(FOUND, SEARCHING)) {
            throw SaveFileFormatException(name, "Mordie's status must be '<N> $FOUND' or '<N> $SEARCHING'")
        }
        return SavedMordie(original, moves, found = keyword == FOUND)
    }

    private fun parseArrangement(name: String, line: String, expectedSize: Int, what: String): IntArray {
        val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val values = IntArray(tokens.size) { i ->
            tokens[i].toIntOrNull() ?: throw SaveFileFormatException(name, "$what value '${tokens[i]}' is not an integer")
        }
        if (values.size != expectedSize) {
            throw SaveFileFormatException(name, "expected $expectedSize $what values, found ${values.size}")
        }
        if (!isArrangement(values, expectedSize)) {
            throw SaveFileFormatException(name, "$what values are not a valid arrangement (each tile must appear exactly once)")
        }
        return values
    }

    private fun isArrangement(values: IntArray, size: Int) = values.size == size && values.toSet() == (0 until size).toSet()

    override fun isValidName(name: String): Boolean = isSafeName(name)

    private fun pathFor(name: String): Path = directory.resolve("$name$EXTENSION")

    /** User input goes straight into a path - this is a real traversal guard, not ceremony. */
    private fun requireSafeName(name: String) {
        require(isSafeName(name)) { "Save name must not be blank, contain a path separator, or contain '..'" }
    }

    private fun isSafeName(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') && !name.contains('\\') &&
            name.split('/', '\\').none { it == ".." }
}
