package viewmodel

/** How deleting a save went (GH-251): [NOT_FOUND] when it was already gone, [ERROR] when storage refused. */
enum class DeleteResult { DELETED, NOT_FOUND, ERROR }
