#!/bin/bash
# GH-174: plays GH-163's dialog flows and GH-164's kept focus from the keyboard in headless Chrome
# (report=keys) and diffs the report against tests/keys_expected.txt, so a change to the page's
# keys, focus, inert or announcements (GH-166) shows up.
#
# Usage: keys_check.sh
#   Builds the bundle first, so it's never stale. Exit 0: as expected; 1: differences, shown as a
#   diff; 2: a failed build or a TIMEOUT. Run by the agent, not CI: it needs Chrome.

set -u

here="$(cd "$(dirname "$0")" && pwd)"
project="$(cd "$here/../../.." && pwd)"
expected="$here/tests/keys_expected.txt"
queries=(
  "report=keys&keys=click:Labels,S-Tab,Tab,Tab,Escape"
  "report=keys&keys=focus:new,Enter,Escape"
  "report=keys&keys=click:Save,Escape"
  # GH-164: focus kept across the redraw, or passed on.
  "report=keys&keys=focus:load,Enter,Escape,Tab"
  "report=keys&keys=focus:arrow-up-0,Enter,Enter,R-Enter,Tab"
  # grid=1 (GH-167): no table and no Read board on the solution screen.
  "report=keys&grid=1&mode=solution&keys=focus:replay-next,Enter,focus:replay-prev,Enter"
  "report=keys&mode=solution&keys=focus:replay-next,Enter,Enter,focus:replay-restart,Enter"
  # GH-166: what the live region says for a new game and a shift.
  "report=keys&keys=click:New,click:5x5,click:▶"
  # GH-167: the board as a table and Read board, pressed, after a shift and under a dialog.
  "report=keys&grid=1&keys=focus:read-board,Enter,click:▶,Enter,click:Labels"
  # Letters, said in the phonetic alphabet by the cells and Read board.
  "report=keys&grid=1&keys=click:Labels,focus:label-group-latin,Enter,Enter,click:OK,focus:read-board,Enter"
  # GH-313: Cyrillic letters, said by the Russian spelling words.
  "report=keys&grid=1&keys=click:Labels,focus:label-group-cyrillic,Enter,Enter,click:OK,focus:read-board,Enter"
  # GH-171: Controls with 6 and with Tab+Enter; its groups' Tab stops and arrows; Escape; OK.
  "report=keys&keys=6,ArrowRight,Tab,ArrowDown,ArrowDown,Tab,Tab,Tab,Escape"
  "report=keys&keys=focus:controls,Enter,ArrowLeft,Enter"
  "report=keys&keys=focus:labels,6,Escape,1,Escape"
  # GH-323: Tile labels' groups: Tab visits the three headers and the open group's one radio; an arrow moves the mark inside the open group; a header opens its group with focus on its first scheme.
  "report=keys&keys=click:Labels,Tab,Tab,Tab,Tab,Tab,Escape"
  "report=keys&keys=click:Labels,ArrowDown,ArrowDown,ArrowUp,focus:label-group-latin,Enter,ArrowDown,Escape"
  # GH-323: a group of 0-based schemes: its header opens it, and its rows are named like the 1-based ones'.
  "report=keys&keys=click:Labels,focus:label-group-numbers_from_0,Enter,ArrowDown,Escape"
  # GH-262: the New version available notice. Update with no moves reloads, so a shift first; the dialog opens on Cancel, Escape and Cancel return to Update.
  "report=keys&update=ready&keys=click:▶,focus:update,Enter,Escape,focus:update,Enter,Tab,Tab,Enter"
  # GH-243: Share opens on Copy link; Tab stays inside it; Escape puts focus back on Share.
  "report=keys&keys=click:Share,Tab,Tab,Tab,Tab,Escape"
  # GH-267: the labels toggle is a check box; Space and Enter flip it, and focus stays on it.
  "report=keys&keys=click:Share,focus:share-labels,Space,Enter,Escape"
  # GH-272: the progress toggle is dimmed and skipped by Tab at 0 moves; after a move it is a Tab stop, and Space and Enter flip it.
  "report=keys&keys=click:Share,Tab,Tab,Tab,Escape"
  "report=keys&keys=click:▶,click:Share,Tab,Tab,Tab,Tab,Escape"
  "report=keys&keys=click:▶,click:Share,focus:share-progress,Space,Enter,Escape"
  # GH-267: a link whose labels differ asks first, focus on Yes; Tab cycles inside; Escape is No; Enter on Yes accepts.
  "report=keys&mode=shared-labels&keys=Tab,Tab,Escape"
  "report=keys&mode=shared-labels&keys=Enter"
  # GH-272: a link that carries progress raises no prompt: the board starts at once, focus stays where it was.
  "report=keys&mode=shared-progress&keys=Tab,Escape"
  # OK in Controls: focus goes to the board, where the digits then work (GH-287: on a computer, whatever opened it).
  "report=keys&keys=6,ArrowRight,click:OK,1,Escape"
  # Each button shows and names its digit, and every digit works (GH-287).
  "report=keys&keys=focus:new,1,Escape,2,3,Escape,focus:load,3,Escape,focus:save,4,Escape,5,Escape,6,Escape,7,Escape,8"
  "report=keys&layout=left&keys=6,Tab,Escape"
  # A held 2 opens Save once and isn't typed into its name field.
  "report=keys&keys=2,R-2,3"
  # The board: a grid, the selected cell its active descendant. Selects wrap; shifts stay put; a held key repeats.
  "report=keys&grid=1&keys=d,s,w,w,R-a,h,ArrowLeft,l,ArrowUp,j"
  "report=keys&grid=1&layout=left&keys=l,ArrowDown,a,W"
  # Board keys only with focus on the board, which is one Tab stop, the arrows out of the order.
  "report=keys&keys=Tab,S-Tab,Tab,w,focus:new,h,Tab"
  # A dialog from the board returns there; a digit on a footer link returns there.
  "report=keys&keys=2,Escape,6,ArrowLeft,click:OK"
  "report=keys&keys=focus:footer-0,2,Escape"
  # GH-277: a new game from the New button gives the board focus, so its keys work; Escape still returns to New.
  "report=keys&grid=1&keys=focus:board,s,click:New,click:5x5,s,ArrowUp,d,ArrowLeft"
  "report=keys&keys=focus:new,Enter,Escape"
  # 0 reads the board wherever focus is, once per press; not under a dialog.
  "report=keys&grid=1&keys=0,R-0,focus:new,0,2,0,Escape"
  # GH-288: the tutorial's panel. from the last arrow, Tab goes Controls, Labels, Skip, then the footer; 4 and 6 open their dialogs and Escape comes back to the panel's button; 1, 2, 3, 5, 7 and 8 do nothing.
  "report=keys&mode=tutorial&keys=focus:arrow-down-3,Tab,Tab,Tab,Tab"
  "report=keys&mode=tutorial&keys=4,Escape,6,Escape,1,2,3,5,7,8"
  "report=keys&mode=tutorial&keys=focus:tutorial-skip,Enter,Escape"
  # Step 5: the solved board's focus falls to Labels, not Skip, so a held Enter can't skip.
  "report=keys&mode=tutorial&step=5&keys=focus:tutorial-labels,Enter,Escape"
  # GH-289: step 6's redraw (3 s after step 5) under an open Labels dialog keeps focus on OK or Cancel, never the page body; Escape then closes it.
  "report=keys&mode=tutorial&step=5&keys=4,focus:dialog-ok,wait:3500,Escape"
  "report=keys&mode=tutorial&step=5&keys=4,focus:dialog-cancel,wait:3500,Escape"
  # GH-289: 8 replays the tutorial at once with no moves, focus on the board. After a move it asks first, opening on Cancel; Escape returns to the board, or to Tutorial for a click on it; a cancelled Save returns to Tutorial.
  "report=keys&keys=8"
  "report=keys&keys=click:▶,8,Tab,Tab,Tab,Escape"
  "report=keys&keys=click:▶,focus:tutorial-replay,Enter,Tab,Tab,Enter,Escape"
  # GH-200: on a non-Latin layout a letter key acts by its position; a Latin one by what it types.
  "report=keys&grid=1&keys=в@KeyD,р@KeyH,ц@KeyW,z@KeyW"
  # The solution screen: the board takes focus and the keys, in place; off the board the keys are nothing.
  "report=keys&grid=1&mode=solution&keys=l,Space,R-Space,Enter,h,r,ArrowDown,w,Tab,l,S-Tab,0"
  "report=keys&layout=left&mode=solution&keys=ArrowRight,d,a,R"
  # GH-275: the language picker is after the game's controls and before the footer; Enter and Space on a toggle switch the language, focus stays on it; under a dialog in German it is inert.
  "report=keys&keys=focus:footer-0,S-Tab"
  "report=keys&keys=focus:language-de,Enter,Space,Tab"
  "report=keys&keys=focus:language-ru,Enter,focus:language-en,Enter"
  # A click on a toggle gives the board focus, so its keys work; a key press leaves focus on the toggle.
  "report=keys&grid=1&keys=click:DE,s,focus:language-ru,Enter,s"
  "report=keys&lang=de&keys=focus:labels,Enter,Escape"
  # GH-275: on a phone the dropdown is the one Tab stop (the toggles are hidden).
  "report=keys&sizes=390x844&keys=focus:language-select,S-Tab,Tab"
)

log="$(cd "$project" && ./gradlew -q :web:jsBrowserDistribution 2>&1)" || {
  echo "$log" >&2
  exit 2
}
# mode=solution plays a random board, so its solution's length and steps vary: those lines keep
# only the shape of what's said, e.g. `Step N of N: LINE N DIR N`. So do the play board's tiles
# on grid lines (GH-167): the numbers and phonetic letters said and counted, and which of the first
# row's cells are in place.
actual="$("$here/measure.sh" "${queries[@]}" | perl -pe '
  s/said "[^"]*\bNow \K\d+/N/;
  s{(?:said|name) "\K[^"]*}{$& =~ s/\d+/N/gr =~ s/\b(?:Row|Column) N (?:left|right|up|down)\b/LINE N DIR/gr}ge if /^\S+ solution /;
  if (/\| grid (?:table|grid) /) { s{(?:said|row1) "\K[^"]*}{$& =~ s/\d+/N/gr =~ s/\b(?:alpha|bravo|charlie|delta|echo|foxtrot|golf|hotel|india|juliet|kilo|lima|mike|november|oscar|papa|quebec|romeo|sierra|tango|uniform|victor|whiskey|x-ray|yankee|zulu)\b/W/gr =~ s/(?:Анна|Борис|Василий|Григорий|Дмитрий|Елена|Женя|Зинаида|Иван|Константин|Леонид|Мария|Николай|Ольга|Павел|Роман|Семён|Тамара|Ульяна|Фёдор|Харитон|Цапля|Человек|Шура|Щука|Эхо|Юрий|Яков)/W/gr =~ s/, in place//gr =~ s/\b(tile|move)s?\b/$1s/gr}ge; s/in-place \K\d+/N/ }')"
if grep -q '^TIMEOUT' <<<"$actual"; then
  echo "$actual" >&2
  exit 2
fi
diff -u "$expected" - <<<"$actual" || exit 1
echo "OK: $(wc -l <"$expected" | tr -d ' ') lines as expected"
