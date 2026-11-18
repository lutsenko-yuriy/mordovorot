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
- **Amendment (2026-10-29, GH-189):** 6 opens a **Controls** dialog in both modes instead of switching mode: `Play with ○ Mouse ○ Keyboard`, `Keyboard keys ○ Right-handed ○ Left-handed`, and the key list for the marked layout. Its button is `[ Controls 6 ]` in both modes, the one mouse-mode button showing its digit (corrected in GH-170: the amendment first said plain `[ Controls ]` in mouse mode, but 6 works there too), and replaces `[ Keyboard 6 ]` / `[ Mouse 6 ]` and the terminal's `[ Keyboard ]`. Switching mode takes three steps (6, pick, OK) in exchange for one button that also shows the controls. The **left-handed** layout mirrors the hands: H/J/K/L or the arrows move the selected tile, A/D shift its row, W/S its column; digits and the solution screen are unchanged. The layout is remembered in both clients. The terminal's key list also names `Exit Esc` (GH-170).
- **Amendment (2026-10-30, GH-171):** on the web, `[ Controls 6 ]` has a row of its own under the toolbar instead of being a fifth toolbar button, and **0** reads the board in keyboard mode (no button or hint; the key list names it).
- **Amendment (2026-10-31, GH-200):** how letters are read differs by client, what they do doesn't. On a non-Latin system keyboard layout the web takes the Latin letter at the key's position (`KeyboardEvent.code`); a key counts as non-Latin when what it types is at or above U+0250, outside U+1E00..U+1EFF, so Latin layouts still act by the letter they type. The terminal gets only characters, so it has a table for Ukrainian and Russian, and its key list names those layouts. A key typing ASCII punctuation on a non-Latin layout (Hebrew's W) stays a known gap: it can't be told from Dvorak's.
- **Amendment (2026-11-02, GH-243):** on the web, **7** opens the Share dialog, from `[ Share 7 ]` after `[ Labels ]` in the toolbar; like the other digits it works only in keyboard mode. The terminal has no Share.
- **Amendment (2026-11-16, GH-289):** on the web, **8** replays the tutorial, from `[ Tutorial 8 ]` beside `[ Give Up 5 ]` in the Give Up row; like the other digits it works on a computer only, and nothing during the tutorial or on the solution screen. The terminal has no tutorial.
- **Amendment (2026-11-17, GH-332):** on the web, **8** opens the alphabet hint, from `[ Hint 8 ]` between `[ Give Up 5 ]` and the tutorial button, and the tutorial replay moves to **9** (`[ Tutorial 9 ]`). Like the other digits it works on a computer only, and is a no-op while dimmed (a number scheme) and during the tutorial or on the solution screen. The terminal has neither.
- **Amendment (2026-11-15, GH-287):** the web has no mode. On a computer every digit works wherever focus is outside dialogs, and every button shows its digit; on touch none works. This reverses the rejection of "digits always on" below, and with it the off switch that WCAG 2.1.4 asks of single-key shortcuts. The risk is to speech-input users, whose dictation can fire a digit; it is accepted for now, with a follow-up open (digits only with board focus, or a Controls toggle) should it matter.
- **Amendment (2026-11-18, GH-349):** on the web, **R** restarts the game, from `[ Restart R ]` after `[ New 1 ]` in the toolbar. It is the one letter on the page outside dialogs, read through the same layout rule as the board's keys (GH-200), on a computer only (Ctrl/Cmd+R stays the browser's reload) and a no-op while the button is dimmed or hidden or the tutorial runs. On the solution screen with the board focused R stays the replay's Restart (the board's keys come first). A held R is one press. It clashes with no board key in either layout. The terminal has no Restart.

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
| Left-handed: numpad bindings | Most laptops have no numpad (GH-189) |
| Left-handed: full key remapping | A settings dialog, clash detection and reset for one need a mirrored preset meets (GH-189) |
| Mode switch cycling Mouse → Keyboard → Keyboard (left-handed) | 6 would stop being a plain toggle; superseded by the Controls dialog anyway (GH-189) |

## Related ticket
GH-169, amended by GH-189 (built in GH-170 terminal, GH-171 web)

## Date
2026-10-29
