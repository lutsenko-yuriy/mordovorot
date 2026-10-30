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
        // Named by its title (GH-163).
        setAttribute("aria-labelledby", TITLE_ID)
        append(text("title", dialog.title).apply { id = TITLE_ID })
        when (dialog) {
            is WebDialog.Size -> append(
                choices(dialog.sizes.map { size -> button("${size}x$size") { game.onSizeChosen(size) }.focusKey(Focus.SizeChoice(size)) }),
                actions(button("Cancel", "secondary") { game.onSizeChosen(null) }),
            )
            is WebDialog.Save -> {
                val name = (element("input") as HTMLInputElement).apply {
                    value = dialog.name
                    setAttribute("aria-label", "Save name")
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

private const val TITLE_ID = "dialog-title"

private fun choices(buttons: List<Element>, kind: String? = null) =
    element("div", listOfNotNull("choices", kind).joinToString(" ")).apply { buttons.forEach { append(it) } }

private fun actions(vararg buttons: Element) = element("div", "actions").apply { append(*buttons) }
