package storage

/** Which save names every [SaveRepository] accepts. A name must not look like a path
 *  (a traversal guard) (GH-110). */
object SaveNames {

    fun isSafe(name: String): Boolean =
        name.isNotBlank() &&
            !name.contains('/') && !name.contains('\\') &&
            name.split('/', '\\').none { it == ".." }

    fun requireSafe(name: String) {
        require(isSafe(name)) { "Save name must not be blank, contain a path separator, or contain '..'" }
    }
}
