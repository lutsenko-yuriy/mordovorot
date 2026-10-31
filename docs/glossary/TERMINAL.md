# Glossary — Terminal

Terminal app only: its dialogs and toolbar. Part of the [glossary](../GLOSSARY.md).

| Term | Definition | Code symbol |
|---|---|---|
| Dialog | A modal box in the TUI (Save, Load, Exit, New-game size picker, Startup restore) that takes over input until confirmed or cancelled | `Dialog`, `DialogLayout` |
| Startup restore prompt | The launch-time offer to restore an existing save before the first move (a TUI dialog) | `restoreOnStartup()` |
| Toolbar | The TUI's row of buttons below the board — `[ New ] [ Save ] [ Load ] [ Exit ]` — plus the input-mode switch row beneath it | `HitTarget.Toolbar*` |
