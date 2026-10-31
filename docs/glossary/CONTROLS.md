# Glossary — Controls

Input and display settings both the terminal and the web client have: input mode, keyboard layouts, selection, tile labels. Part of the [glossary](../GLOSSARY.md).

| Term | Definition | Code symbol |
|---|---|---|
| Controls dialog | The dialog `[ Controls ]` / 6 opens in both clients and modes: picks the input mode (Mouse or Keyboard) and the keyboard layout, and lists the keys of the marked layout. Replaces the mode-switch button (GH-189; built in GH-170, GH-171) | — |
| Input mode | Whether the player drives the game with the Mouse or the Keyboard, in both the terminal and the web client. Chosen in the Controls dialog; switchable mid-session without losing the game | `InputMode` |
| Key hints | Keyboard mode's on-page reminder of the board keys for the active layout, ordered by hand: in the terminal beside the board, on the web two lines under it, the keys above what they do (GH-170, GH-171) | — |
| Keyboard layout | Which hand does which job in keyboard mode: Right-handed (default; WASD select, HJKL/arrows shift) or Left-handed (HJKL/arrows select, WASD shift). Chosen in the Controls dialog, remembered as a preference (GH-189) | — |
| Selected tile | Keyboard mode's selection under the shared keyboard scheme: one tile, so a selected row and column at once; WASD move it, HJKL/arrows shift its row (←/→) or column (↑/↓), and it stays put after a shift. Replaces line selection (GH-169; built in GH-170, GH-171) | — |
| System keyboard layout | The layout the player's OS types with (Latin, Ukrainian, Russian, ...). Not the same as the game's Keyboard layout, which is which hand does which job. On the web any non-Latin one plays by key position; the terminal knows Ukrainian and Russian (GH-200) | — |
| Tile labels | How tiles are labelled, one of six schemes: Numbers (default, `1..side²`), Numbers from 0, Pairs (`23`), Pairs from 0 (`12`), Letters (`G`), Letter pairs (`BC`). Chosen in the Tile labels dialog; remembered as a preference and stored in saves, a save's labels winning for that game (GH-139); Letters only labels up to 5x5 - on 6x6 and larger Numbers stand in and the preference is kept (GH-160) | — |
