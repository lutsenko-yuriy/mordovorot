package storage

/** A plain string store for player preferences (GH-139), e.g. the chosen tile labels. Keys and
 *  values are single-line and keys are non-empty with no `=`; [set] may throw
 *  [IllegalArgumentException] otherwise. Storage errors propagate as the platform's own
 *  exception, like [SaveRepository]. */
interface PreferenceRepository {

    /** `null` when nothing is stored under [key]. */
    fun get(key: String): String?

    fun set(key: String, value: String)
}
