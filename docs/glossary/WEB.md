# Glossary — Web

Web client only: the page, its footer, drag, large boards and the public site. Part of the [glossary](../GLOSSARY.md).

| Term | Definition | Code symbol |
|---|---|---|
| Drag | Web client only: pressing on a tile and dragging slides its row (horizontal drag) or column (vertical drag) under the pointer, snapping to whole tiles on release. Each tile of distance is one shift; the whole drag is one move (GH-110) | `LineDrag` |
| Footer | Web client only: one muted line at the bottom of the page with the app version and the Loopover credit (Cary Huang, links to the original and loopover.xyz) (GH-111) | — |
| Public site | The web client published on GitHub Pages at `https://lutsenko-yuriy.github.io/mordovorot/`, updated on every merge to `master` whose build and tests pass (GH-111) | — |
| Share link | Web client only: a link to the public site that carries a game's starting board (its original arrangement; the current board when the start is unknown), shown in the Share dialog with a QR code of the same link and a Copy link button. Opening it starts that board as a fresh game with its own Mordie race, replacing the current game, and the link is then cleared from the address bar. It carries no tile labels or moves | `ShareLink`, `BoardLinkFormat` |
| Shared labels | Web client only: the tile labels a share link can carry when the Share dialog's `Include my tile labels` toggle is on (the labels shown on the sender's board). A receiver is asked whether to use them; Yes applies them to that game only and never changes the receiver's preference. Labels that can't be read are ignored | — |
| Unsaved progress | At least one shift since the last New game, successful save or successful load. The web client asks before the tab is closed or reloaded with it (GH-110) | `UnsavedProgress` |
| Update notice | Web client only: when a new version of the game is ready, the footer's credit text is replaced by `New version available. Update`, `Update` being a bold, underlined link. Clicking it reloads the page to the new version, after a confirmation if there is unsaved progress | `WebUpdateFlows`, `FooterPart.UpdateLink`, `AppUpdates` |
| Very large | Web client only: board sizes 7x7 to 9x9, offered in their own section of the New game dialog under the 3x3 to 6x6 row, with a warning that they are harder to solve and their tiles may be too small to use comfortably. Mordie plays them like any other size (GH-225) | — |
| Web client | The browser version of the game, played with the mouse or, since GH-171, the keyboard, built on the shared modules, with its saves kept in the browser (GH-110) | — |
