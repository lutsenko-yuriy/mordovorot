package storage

/** Thrown when a save file's contents can't be parsed into a [SavedBoard]. */
open class SaveFileFormatException(name: String, reason: String) :
    RuntimeException("Save '$name' is corrupted: $reason")

/** A save whose first character no version of the format claims: a later build wrote it (GH-313). */
class SaveFromNewerVersionException(name: String) :
    SaveFileFormatException(name, "it was made by a newer version")
