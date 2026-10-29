# ADR-0003 — drive both clients with one keyboard scheme: WASD select a tile, HJKL/arrows shift, digits for the toolbar

## Status
`accepted`

## Context
The terminal's keyboard mode (GH-18, GH-101) selected a row or a column, one of them active, and put the toolbar on F-keys. Porting it to the web as it was would not work: the browser and OS take several F-keys (F5 reloads the page; Mac F-keys are media keys without fn). The web already had a Tab/Enter model with no mode (GH-163, GH-164). Keys had to suit both a terminal (no Cmd combinations, Ctrl+S/Q often swallowed) and a browser, and stay clear of screen readers' single-key navigation.

## Decision
- **Board:** the selection is one tile, a selected row and column at once. **W/A/S/D** move it (wrapping); **H/←**, **L/→** shift its row; **K/↑**, **J/↓** shift its column. It stays put after a shift. Left hand selects, right hand shifts.
- **Toolbar:** **1** New, **2** Save, **3** Load, **4** Labels, **5** Give Up / Mordie's solution, **6** switch input mode; **Escape** stays Exit in the terminal. Digits are free in both clients and safe from the browser and OS.
- **Modes:** both clients have a keyboard mode. In it every button shows its digit and all digits work. In mouse mode only 6 works, shown as `[ Keyboard 6 ]`. Mouse mode is thus the off switch that WCAG 2.1.4 asks of single-key shortcuts ([W3C: Understanding SC 2.1.4 Character Key Shortcuts](https://www.w3.org/WAI/WCAG21/Understanding/character-key-shortcuts.html)). Screen readers' browse modes catch digits for heading navigation before the page sees them ([NVDA User Guide: Browse Mode](https://www.nvaccess.org/files/nvda/documentation/userGuide.html#BrowseMode)), so they don't clash.
- **Web:** a fifth toolbar button `[ Keyboard 6 ]` / `[ Mouse 6 ]` after `[ Labels ]`, hidden where the main input is touch; the mode is remembered between visits.
- **Solution screen:** as before, with H/L also Prev/Next and K/J also scrolling: HJKL are the arrows' twins everywhere.
- Dialogs keep their own keys while open.

## Alternatives considered
| Option | Why not chosen |
|---|---|
| Keep the row-or-column line selection (GH-101) | Needs an active-line switch; a selected tile drives both axes with fewer keys |
| Tile cursor moved by arrows, Shift+arrow to shift | Leans on modifiers; WASD/HJKL split the hands with plain keys |
| Toolbar on F-keys | The browser and OS take them (F5 reloads; Mac media keys) |
| Toolbar on Shift+letter or free lowercase letters | Caps Lock confuses uppercase with board keys; free letters (N, O, P…) need memorising |
| Digits always on in mouse mode too | No way to turn single-key shortcuts off (WCAG 2.1.4) |
| Web without a mode (keys whenever the board has focus) | User preferred matching the terminal's mode |
| Enter web keyboard mode by pressing 6 twice, no button | Hard to discover; a visible button shows the way in |

## Related ticket
GH-169 (built in GH-170 terminal, GH-171 web)

## Date
2026-10-29
