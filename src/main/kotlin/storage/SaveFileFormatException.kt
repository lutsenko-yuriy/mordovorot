package storage

/** Thrown when a save file's contents can't be parsed into a [SavedBoard]. */
class SaveFileFormatException(name: String, reason: String) :
    RuntimeException("Save '$name' is corrupted: $reason")
