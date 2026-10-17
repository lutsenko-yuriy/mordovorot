import kotlinx.browser.document

// GH-110 WU1: a placeholder until the game is wired up in WU4.
fun main() {
    document.getElementById("app")?.textContent = "The browser game is on its way."
}
