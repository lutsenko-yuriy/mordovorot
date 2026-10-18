package web

/** How the page arranges itself (GH-110): the board beside the rest when [WIDE], stacked when [TALL]. */
enum class Layout { WIDE, TALL }

/** A square window counts as tall. */
fun layoutOf(width: Int, height: Int): Layout = if (width > height) Layout.WIDE else Layout.TALL
