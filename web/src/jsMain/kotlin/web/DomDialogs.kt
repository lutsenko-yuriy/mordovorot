package web

import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.KeyboardEvent
import viewmodel.UnsolvableSaveChoice

/** The modal dialog for [dialog], its answers going to [game] (GH-110). [onSaveWarning] gets the
 *  Save dialog's warning line, which [DomRenderer] updates in place while the player types. */
internal fun dialogElement(dialog: WebDialog, game: WebGame, onSaveWarning: (Element) -> Unit): Element =
    element("div", "dialog").apply {
        setAttribute("role", "dialog")
        setAttribute("aria-modal", "true")
        when (dialog) {
            is WebDialog.Size -> append(
                text("title", "New game size"),
                choices(dialog.sizes.map { size -> button("${size}x$size") { game.onSizeChosen(size) } }),
                actions(button("Cancel", "secondary") { game.onSizeChosen(null) }),
            )
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    setAttribute("aria-label", "Save name")
                    addEventListener("input", { game.onSaveNameTyped(value) })
                    addEventListener("keydown", {
                        // An Enter that confirms an IME composition isn't the player's Save.
                        val key = it as KeyboardEvent
                        if (key.key == "Enter" && !key.isComposing && key.keyCode != 229) game.onSaveAnswered(value)
                    })
                }
                val warning = text("warning", dialog.warning ?: "")
                onSaveWarning(warning)
                append(
                    text("title", "Save game"),
                    name,
                    warning,
                    actions(
                        button("Cancel", "secondary") { game.onSaveAnswered(null) },
                        button("Save") { game.onSaveAnswered(name.value) },
                    ),
                )
            }
            is WebDialog.Load -> {
                append(text("title", if (dialog.restore) "Restore a saved game?" else "Load game"))
                if (dialog.saves.isEmpty()) append(text("message", "No saves found."))
                else append(choices(dialog.saves.map { save -> button(save.label) { game.onLoadChosen(save.name) } }, "list"))
                append(actions(button("Cancel", "secondary") { game.onLoadChosen(null) }))
            }
            is WebDialog.Unsolvable -> append(
                text("title", "Can't be solved"),
                text("message", dialog.message),
                actions(
                    button("New game", "secondary") { game.onUnsolvableChoice(UnsolvableSaveChoice.NEW_GAME) },
                    button("Repair & load") { game.onUnsolvableChoice(UnsolvableSaveChoice.REPAIR) },
                ),
            )
            is WebDialog.Labels -> append(
                text("title", "Tile labels"),
                choices(dialog.options.map { labelRow(it, it.labels == dialog.selected, game) }, "list labels").apply {
                    setAttribute("role", "radiogroup")
                    setAttribute("aria-label", "Tile labels")
                },
                actions(
                    button("Cancel", "secondary") { game.onLabelsAnswered(ok = false) },
                    button("OK") { game.onLabelsAnswered(ok = true) },
                ),
            )
            WebDialog.GiveUp -> append(
                text("title", "Give up and see Mordie's solution?"),
                actions(
                    button("No", "secondary") { game.onGiveUpAnswered(false) },
                    button("Yes") { game.onGiveUpAnswered(true) },
                ),
            )
        }
    }

/** A radio-role button, since the page is rebuilt every frame and a native radio would lose its state. */
private fun labelRow(option: LabelOption, checked: Boolean, game: WebGame) =
    button("", if (checked) "checked" else null) { game.onLabelsSelected(option.labels) }.apply {
        setAttribute("role", "radio")
        setAttribute("aria-checked", checked.toString())
        if (!option.available) {
            setAttribute("disabled", "")
            setAttribute("aria-disabled", "true")
        }
        append(
            element("span", "mark").apply { textContent = if (checked) "●" else "○" },
            element("span", "name").apply { textContent = option.title },
            element("span", "sample").apply { textContent = option.sample.joinToString(" ") { it.padEnd(2) } },
        )
    }

private fun choices(buttons: List<Element>, kind: String? = null) =
    element("div", listOfNotNull("choices", kind).joinToString(" ")).apply { buttons.forEach { append(it) } }

private fun actions(vararg buttons: Element) = element("div", "actions").apply { append(*buttons) }
