package web

/** The page's address bar (GH-243): where a share link arrives, and is cleaned away. */
interface PageAddress {
    /** The page's own origin and path, the base of a share link. */
    fun base(): String

    /** The address's fragment, with its `#`; empty when it has none. */
    fun fragment(): String

    /** Drops the fragment from the address without adding a history entry or reloading. */
    fun clearFragment()
}
