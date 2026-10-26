# Web harness (GH-147, GH-148, GH-156, GH-174)

Checks that the web game fits phone screens, headless, compares its computed styles against master,
checks it from the keyboard and serves it to a real phone.

## Headless measurements

```
./gradlew :web:jsBrowserDistribution
scripts/web/harness/measure.sh "v=mouse" "v=touch&mode=solution&boards=3"
```

One line per case: `OK`/`BAD`, then the page and window sizes, the toolbar's height, width and
position, the footer's height, the tiles' position and size, Mordie's spot, the panel's height (wide
only), how far the steps scroll sideways (solution screen), the dialog's height against its content
(a taller dialog scrolls inside itself) and the furthest any control goes past any of the window's
four edges. `OK` means the page is exactly the window's size, nothing is past an edge, the toolbar
has at most `rows` rows, the footer one line and the steps don't scroll sideways. A dialog's own
buttons don't count, since they scroll inside it; the dialog box itself does.

Query options (harness.html):

| Option | Default | Meaning |
|---|---|---|
| `v` | `mouse` | `mouse`, `touch` (the `pointer: coarse` rules always on); `master`, `master-touch` with `--baseline` |
| `sizes` | `portrait` | comma-separated `WxH`, or a preset: `portrait` (SE through Pro Max), `landscape` (the same turned, plus 750x390 and 814x430: the 14 and Pro Max less Safari's notch insets, GH-148) |
| `boards` | `3,4,5` | board sides |
| `mode` | `play` | `play`, `dialog` (Tile labels open) or `solution` (shifts until Give Up lights, then gives up) |
| `shorter` | `0` | px taken off each height, standing in for the browser's own toolbars (try `100`) |
| `rows` | `1` | toolbar rows allowed (`2` under 300px wide) |
| `report` | `fit` | `fit` (one line per case, above), `styles` (every element's computed styles, for `styles_diff.sh`) or `keys` (below; `sizes` then defaults to `1200x800` and `boards` to `4`) |
| `keys` | | with `report=keys`, the steps to play, comma-separated (below) |

To compare against master, build master's bundle into another directory and pass it with
`--baseline <dir>`; positions in the `master` lines should match the `mouse` ones where nothing
was meant to change. Keep each query to a few cases on the solution screen: Mordie needs a few
seconds each, and a query gives up after 10 minutes (`TIMEOUT`). The 5×5 solution screen can't be reached: Mordie
doesn't reveal its solution within the harness's 80 shifts, so use 3×3 and 4×4 there.

## Computed-style diff

```
scripts/web/harness/styles_diff.sh --build-baseline ../master-dist
```

Compares every element's computed styles in the branch's bundle against a baseline bundle, so a
CSS refactor can show the game looks the same: `measure.sh` checks only a few sizes and positions,
which two different cascades can share. It builds the branch's bundle first, so a CSS edit is never
compared from an old build. `--build-baseline` builds `origin/master` (or
`--build-baseline=<ref>`) in a temporary worktree into the given directory. It stamps the bundle
with its commit (`.commit`), so later runs reuse it until the ref moves, then build again;
`git fetch` first so `origin/master` is current. It won't overwrite a non-empty directory it
didn't build. Without the flag, pass any bundle directory as is.

It makes four variants: `mouse` and `touch` from the branch, `master` and `master-touch` from the
baseline. Each gets a seeded `Math.random`, so the scramble and Mordie's solution match across
bundles. It then compares `mouse` with `master` and `touch` with `master-touch` case by case,
element by element (`TAG.class.class`), as property maps, so the order Chrome lists them in
doesn't count. `<link>` elements are skipped, and an element on one side only is reported as such.
By default it runs the play, dialog and solution screens on a 4×4 board at 390x844, 844x390 and
1200x800: 18 compared cases, a few minutes. Other queries go after the directory, without `v` and
`report`, which it sets itself.

It prints each differing element with each property's old and new values, then a total. Elements
sharing a selector are numbered by their place among them in that case, on the branch's side
(`DIV.step[3]`), or the baseline's for one only there (`-`); a run that changed the same way is one
entry (`DIV.step[1-6]`), and `inline-size` and the like are left out when they only repeat `width`. Exit 0
means no differences; 1 means differences, or a case on one side only; 2 means a usage error, a
failed build or a `TIMEOUT`. It serves on port 8767 (`STYLES_DIFF_PORT`), so it can run
next to `measure.sh` (8765) and `serve_lan.sh` (8766).

Before calling a difference a regression, rerun the cases it's in. The harness waits for
animations to finish (up to 5s) before reading the styles, but a case can still be caught midway
(GH-153: a dialog's fade gave `opacity` 0 or 0.62 against 1). Under load (next to a build or
`measure.sh`, or with the machine asleep partway) a scroll box can also come out 15px narrower, a
scrollbar's width, and Mordie may not reveal its solution in time, so a `solution` case is missing
its `[solution]` mark on one side. Both went away when rerun alone (GH-156). `::before` and
`::after` aren't compared.

The compare logic and `serve_lan.sh`'s rebuild are unit-tested: `python3 -m unittest discover -s
scripts/web/harness/tests -t scripts/web/harness`.

## From the keyboard

```
scripts/web/harness/measure.sh "report=keys&keys=click:Labels,Tab,S-Tab,Escape"
scripts/web/harness/keys_check.sh
```

`report=keys` plays the steps in `keys` against the screen `mode` leaves, and prints a line for
that screen, then one after each step, once the page has redrawn:

```
mouse play 1200x800 4x4 | S-Tab page -> focus BUTTON "OK" role button name "OK" | dialog "Tile labels" | ring solid | keyboard yes | inert DIV.stage DIV.panel FOOTER.footer
```

A step is `click:<button text>` (a mouse click), `focus:<data-focus key>`, or a key: `Tab`,
`S-Tab` (Shift+Tab), `Escape`, `Enter`, `Space`, `ArrowUp` and the like, sent as a `keydown` to
the focused control. A synthetic key gets no default action, so when the page doesn't take it
(`preventDefault`), the harness does what the browser would: Tab moves to the next control in
DOM order (the page sets no positive `tabindex`) that isn't disabled, hidden or `inert`, and
leaves the page past either end; Enter and Space click a button. The step's mark says which:
`page`, `native`, or `-` (a click, a `focus:`, or a key neither acted on).

After the arrow: the focused control's `data-focus` key (or its tag and text), its role,
accessible name (`aria-label`, `aria-labelledby` or its text) and `checked`; the open dialog's
name; the ring, the focused control's computed `outline-style` (`:focus-visible` matching
misled in GH-163); whether the page has seen a key (`.keyboard`, which turns the rings on); and
the `inert` elements.

Headless can't show what VoiceOver says or how a ring looks; those stay a short checklist for
the user (`docs/workflows/FEATURE.md` step 10.0).

`keys_check.sh` builds the bundle, plays GH-163's dialog flows (Labels with Tab and Shift+Tab
wrapping, New from Enter, Save, each closed with Escape) and diffs the lines against
`tests/keys_expected.txt`. Exit 0 means as expected, 1 differences (the diff is printed), 2 a
failed build or a `TIMEOUT`. It needs Chrome, so the agent runs it rather than CI. When a change
to the page's keys or focus is meant, update the expected file and check each changed line.

## On a real phone

The user runs `! scripts/web/harness/serve_lan.sh` (auto mode blocks the agent from serving to
the network); it prints the address to open on a phone on the same Wi-Fi, and a QR code for the
phone's camera when `qrencode` is installed (`brew install qrencode`), so the phone's browser can't
swap in an older address from its history (GH-148). Ctrl+C stops it.

It builds the bundle first unless the bundle was built from `HEAD` with a clean tree: each build
stamps the bundle with its commit (`.commit`), and uncommitted changes always rebuild (GH-160: the
phone showed a bundle older than the fix it was meant to check). `SERVE_LAN_DRY_RUN=1` stops it
before it serves; `tests/test_serve_lan.py` uses that.
