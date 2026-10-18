package web

import analytics.NoopAnalyticsService
import board_model.BoardImpl
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import mordie.BackgroundMordieSearch
import mordie.Mordie
import storage.SaveNames
import storage.SaveRepository
import storage.SavedBoard
import storage.SavedMordie

/** The page's composition root (GH-110). */
fun main() {
    val root = document.getElementById("app") ?: return
    val renderer = DomRenderer(root)
    renderer.game = startWebGame(
        BoardImpl(),
        NoSaves,
        NoopAnalyticsService(),
        Mordie(BackgroundMordieSearch.Factory()),
        renderer,
        MainScope(),
        layoutOf(window.innerWidth, window.innerHeight),
    )
}

/** Until WU5 keeps saves in localStorage: there are none, and the page has no Save button. */
private object NoSaves : SaveRepository {
    override fun listSaves(): List<String> = emptyList()
    override fun exists(name: String) = false
    override fun save(name: String, state: IntArray, squareSide: Int, moveCount: Int, mordie: SavedMordie?) =
        throw UnsupportedOperationException("The page can't save yet")
    override fun isValidName(name: String) = SaveNames.isSafe(name)
    override fun load(name: String): SavedBoard? = null
}
