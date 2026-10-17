package storage

/** Which save names every [SaveRepository] accepts. On the terminal a name goes straight into a
 *  path, so this is a real traversal guard; the browser rejects the same names (GH-110). */
object SaveNames {

    fun isSafe(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') && !name.contains('\\') &&
            name.split('/', '\\').none { it == ".." }

    fun requireSafe(name: String) {
        require(isSafe(name)) { "Save name must not be blank, contain a path separator, or contain '..'" }
    }
}
