package web

import org.w3c.dom.Element
import texts.texts
import web.a11y.focusKey
import web.a11y.hiddenFromReaders
import web.drag.Axis

// GH-288: what the tutorial adds to the page: Mordie's bubble and its buttons, and the asked line's highlight.

/** Mordie's [guidance] in a bubble, a computer's with its ways lines under the text (GH-290), over `[ Controls 6 ] [ Labels 4 ] [ Skip ]`, which stand where the toolbar does.
 *  A phone has no Controls button. [game] is read only on a click, as in [giveUpRow]. */
internal fun guidancePanel(guidance: Guidance, state: WebState, game: () -> WebGame): Element = element("div", "guidance").apply {
    val off = state.dialog != null
    fun tutorialButton(label: String, focus: Focus, digit: Int?, onClick: () -> Unit) =
        button(label, onClick = onClick).focusKey(focus).withDigit(digit).also { it.disabled = off }
    append(
        element("div", "bubble").apply {
            append(element("span", "name").apply { textContent = texts().page.mordieName }, " ${guidance.text}")
            if (guidance.ways.isNotEmpty()) append(element("div", "ways").apply { guidance.ways.forEach { append(element("div", "way").apply { textContent = it }) } })
        },
        element("div", "guidance-buttons").apply {
            if (guidance.controls) append(tutorialButton(texts().page.tutorialControls, Focus.TutorialControls, state.digitOf(ToolbarButton.CONTROLS)) { game().tutorial.onControls() })
            append(
                tutorialButton(texts().page.tutorialLabels, Focus.TutorialLabels, state.digitOf(ToolbarButton.LABELS)) { game().tutorial.onLabels() },
                tutorialButton(texts().page.tutorialSkip, Focus.TutorialSkip, null) { game().tutorial.onSkip() },
            )
        },
    )
}

/** The dashed outline around the asked line, in the board's middle cell as the selection's outlines are. Seen, not read. */
internal fun tutorialHighlight(highlight: TutorialHighlight?) = element("div", "tutorial-line").apply {
    hiddenFromReaders(this)
    append(element("div", "mark"))
    placeHighlight(this, highlight)
}

internal fun placeHighlight(line: Element, highlight: TutorialHighlight?) {
    if (highlight == null) {
        line.setAttribute("hidden", "")
        return
    }
    line.removeAttribute("hidden")
    line.className = if (highlight.axis == Axis.ROW) "tutorial-line row" else "tutorial-line column"
    line.setAttribute("style", "--line: ${highlight.line};")
}
