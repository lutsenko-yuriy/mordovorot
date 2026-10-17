package viewmodel.usecase

/** How a load ended (GH-41): [EXIT_CHOSEN] is Exit picked for an unsolvable save, which the
 *  viewmodel hands to its `UnsolvableSaveExit` once the load has returned. */
internal enum class LoadResult { LOADED, NOT_LOADED, EXIT_CHOSEN }
