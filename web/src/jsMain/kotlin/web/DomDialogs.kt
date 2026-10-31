package web

import kotlinx.browser.document
import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import viewmodel.KeyboardLayout
import viewmodel.UnsolvableSaveChoice
import web.a11y.asDialog
import web.a11y.asRadio
import web.a11y.asRadioGroup
import web.a11y.asRadioGroupLabelledBy
import web.a11y.focusKey
import web.a11y.hiddenFromReaders
import web.a11y.named

/** The modal dialog for [dialog], its answers going to [game] (GH-110). [onSaveWarning] gets the
 *  Save dialog's warning line, which [DomRenderer] updates in place while the player types. */
internal fun dialogElement(dialog: WebDialog, game: WebGame, onSaveWarning: (Element) -> Unit): Element =
    element("div", "dialog").apply {
        val title = text("title", dialog.title)
        asDialog(this, title)
        append(title)
        when (dialog) {
            is WebDialog.Size -> append(
                choices(dialog.sizes.map { size -> button("${size}x$size") { game.onSizeChosen(size) }.focusKey(Focus.SizeChoice(size)) }),
                actions(button("Cancel", "secondary") { game.onSizeChosen(null) }),
            )
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    named(this, "Save name")
                    focusKey(Focus.SaveName)
                    addEventListener("input", { game.onSaveNameTyped(value) })
                    addEventListener("keydown", {
                        // An Enter that confirms an IME composition isn't the player's Save.
                        val key = it as KeyboardEvent
                        if (key.key == "Enter" && !key.composing) game.onSaveAnswered(value)
                    })
                }
                val warning = text("warning", dialog.warning ?: "")
                onSaveWarning(warning)
                append(
                    name,
                    warning,
                    actions(
                        button("Cancel", "secondary") { game.onSaveAnswered(null) },
                        button("Save") { game.onSaveAnswered(name.value) },
                    ),
                )
            }
            is WebDialog.Load -> {
                if (dialog.saves.isEmpty()) append(text("message", "No saves found."))
                else append(choices(dialog.saves.mapIndexed { i, save ->
                    button(save.label) { game.onLoadChosen(save.name) }.apply { if (i == 0) focusKey(Focus.FirstSave) }
                }, "list"))
                append(actions(button("Cancel", "secondary") { game.onLoadChosen(null) }.focusKey(Focus.DialogCancel)))
            }
            is WebDialog.Unsolvable -> append(
                text("message", dialog.message),
                actions(
                    button("New game", "secondary") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) },
                    button("Repair & load") { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) }.focusKey(Focus.Repair),
                ),
            )
            is WebDialog.Labels -> append(
                *listOfNotNull(dialog.message?.let { text("message", it) }).toTypedArray(),
                choices(dialog.options.map { labelRow(it, it.labels == dialog.selected, game) }, "list labels").also { asRadioGroup(it, "Tile labels") },
                actions(
                    button("Cancel", "secondary") { game.onLabelsAnswered(ok = false) },
                    button("OK") { game.onLabelsAnswered(ok = true) },
                ),
            )
            is WebDialog.Controls -> append(
                radios("Play with", PlayMode.entries.map { mode ->
                    radio(mode.title, mode == dialog.mode, Focus.ModeOption(mode)) { game.controls.onMode(mode) }
                }),
                radios("Keyboard keys", KeyboardLayout.entries.map { layout ->
                    radio(layout.title, layout == dialog.layout, Focus.LayoutOption(layout)) { game.controls.onLayout(layout) }
                }),
                element("div", "key-list").apply { dialog.keyList.forEach { append(text("key-line", it)) } },
                actions(
                    button("OK") { game.controls.onAnswered(ok = true) },
                    button("Cancel", "secondary") { game.controls.onAnswered(ok = false) },
                ),
            )
            WebDialog.GiveUp -> append(
                actions(
                    button("No", "secondary") { game.solution.onGiveUpAnswered(false) }.focusKey(Focus.No),
                    button("Yes") { game.solution.onGiveUpAnswered(true) },
                ),
            )
        }
    }

/** A radio-role button, since the page is rebuilt every frame and a native radio would lose its state. */
private fun labelRow(option: LabelOption, checked: Boolean, game: WebGame) =
    button("", if (checked) "checked" else null) { game.onLabelsSelected(option.labels) }.focusKey(Focus.Label(option.labels)).apply {
        asRadio(this, checked, unavailable = !option.available)
        append(
            element("span", "mark").apply { textContent = if (checked) "●" else "○" },
            element("span", "name").apply { textContent = option.title },
            element("span", "sample").apply { textContent = option.sample.joinToString(" ") { it.padEnd(2) } },
        )
    }

/** A group of radios, one Tab stop: the arrows move its mark (DomKeyboard). */
private fun radios(title: String, options: List<Element>) = element("div", "radios").apply {
    val titleId = "radios-${title.lowercase().replace(' ', '-')}"
    asRadioGroupLabelledBy(this, titleId)
    append(text("radios-title", title).apply { id = titleId })
    append(element("div", "options").apply { options.forEach { append(it) } })
}

/** Only the [checked] radio is a Tab stop. */
private fun radio(title: String, checked: Boolean, focus: Focus, onClick: () -> Unit) =
    button("", if (checked) "checked" else null, onClick).focusKey(focus).apply {
        asRadio(this, checked, rovingTabStop = true)
        append(
            hiddenFromReaders(element("span", "mark").apply { textContent = if (checked) "●" else "○" }),
            element("span", "name").apply { textContent = title },
        )
    }

/** Escape cancels the open dialog. An Escape that drops an IME composition isn't the player's, and
 *  a held one would cancel the next dialog too (Restore -> the size picker). [game] is read only
 *  on a key, as in [web.replayButtons]. */
internal fun listenForEscape(root: Element, game: () -> WebGame) {
    document.addEventListener("keydown", { event ->
        val key = event as KeyboardEvent
        if (key.key == "Escape" && !key.composing && !key.repeat && root.querySelector(".dialog") != null) {
            key.preventDefault()
            game().onEscape()
        }
    })
}

private fun choices(buttons: List<Element>, kind: String? = null) =
    element("div", listOfNotNull("choices", kind).joinToString(" ")).apply { buttons.forEach { append(it) } }

private fun actions(vararg buttons: Element) = element("div", "actions").apply { append(*buttons) }
