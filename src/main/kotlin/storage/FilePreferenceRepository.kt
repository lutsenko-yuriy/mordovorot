package storage

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

/** The terminal's preferences (GH-139): `key=value` lines in `preferences`, beside the saves in
 *  [directory]. It isn't a `.save` file, so it is never listed as one. Lines it doesn't know are
 *  kept, so a later build's preferences survive. */
class FilePreferenceRepository(private val directory: Path = FileSaveRepository.defaultDirectory()) : PreferenceRepository {

    private val file: Path get() = directory.resolve("preferences")

    override fun get(key: String): String? =
        lines().firstNotNullOfOrNull { line -> line.removePrefix("$key=").takeIf { it != line } }

    override fun set(key: String, value: String) {
        val entry = "$key=$value"
        val lines = lines()
        val at = lines.indexOfFirst { it.startsWith("$key=") }
        val updated = if (at < 0) lines + entry else lines.toMutableList().apply { set(at, entry) }

        directory.createDirectories()
        // A temp file and an atomic move, like FileSaveRepository, so a failed write can't truncate it.
        val temp = Files.createTempFile(directory, "preferences-", ".tmp")
        try {
            temp.writeText(updated.joinToString("") { "$it\n" })
            Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Files.deleteIfExists(temp)
            throw e
        }
    }

    private fun lines(): List<String> = if (file.exists()) file.readLines() else emptyList()
}
