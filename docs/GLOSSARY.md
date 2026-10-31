# Glossary

Canonical domain terms for this project, kept in one file per domain. Skills and documentation use these names consistently.
When a new term is introduced during a `brief` session, add it to its domain's file before creating the ticket.

## Domains

| File | What belongs there |
|---|---|
| [glossary/CORE.md](glossary/CORE.md) | The board, its moves, Mordie and saves: terms every client shares |
| [glossary/TERMINAL.md](glossary/TERMINAL.md) | A term defined for the terminal UI (its dialogs, toolbar) |
| [glossary/CONTROLS.md](glossary/CONTROLS.md) | Input and display settings both clients have: input mode, keyboard layouts, selection, tile labels |
| [glossary/WEB.md](glossary/WEB.md) | A term defined for the web client (page, footer, drag, large boards, public site) |
| [glossary/ACCESSIBILITY.md](glossary/ACCESSIBILITY.md) | Screen-reader and text-size support |

When two files fit, the more specific domain wins (a web-only screen-reader term goes in `glossary/ACCESSIBILITY.md`). A term that fits no file: ask the user, then either add a new domain file with a row here, or put it in `glossary/CORE.md`.

## Known aliases to avoid

| Avoid | Use instead |
|---|---|
| grid, matrix | board |
| rotate | shift |
| win, correct state | solved |
| line selection | selected tile (a retired term: `glossary/CONTROLS.md` keeps its row as history) |
