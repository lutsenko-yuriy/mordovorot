# Phone fit (GH-147, GH-148, GH-156)

Checks that the web game fits phone screens, headless, compares its computed styles against master,
and serves it to a real phone.

## Headless measurements

```
./gradlew :web:jsBrowserDistribution
scripts/web/phone_fit/measure.sh "v=mouse" "v=touch&mode=solution&boards=3"
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
| `report` | `fit` | `fit` (one line per case, above) or `styles` (every element's computed styles, for `styles_diff.sh`) |

To compare against master, build master's bundle into another directory and pass it with
`--baseline <dir>`; positions in the `master` lines should match the `mouse` ones where nothing
was meant to change. Keep each query to a few cases on the solution screen: Mordie needs a few
seconds each, and a query gives up after 10 minutes (`TIMEOUT`). The 5×5 solution screen can't be reached: Mordie
doesn't reveal its solution within the harness's 80 shifts, so use 3×3 and 4×4 there.

## Computed-style diff

```
./gradlew :web:jsBrowserDistribution
scripts/web/phone_fit/styles_diff.sh --build-baseline ../master-dist
```

Compares every element's computed styles in the branch's bundle against a baseline bundle, so a
CSS refactor can show the game looks the same: `measure.sh` checks only a few sizes and positions,
which two different cascades can share. `--build-baseline` builds `origin/master` (or
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

It prints each differing element with each property's old and new values, then a total. Exit 0
means no differences; 1 means differences, or a case on one side only; 2 means a usage error, a
failed baseline build or a `TIMEOUT`. It serves on port 8767 (`STYLES_DIFF_PORT`), so it can run
next to `measure.sh` (8765) and `serve_lan.sh` (8766).

Before calling a difference a regression, rerun the cases it's in. The harness waits for
animations to finish (up to 5s) before reading the styles, but a case can still be caught midway
(GH-153: a dialog's fade gave `opacity` 0 or 0.62 against 1). Under load (next to a build or
`measure.sh`, or with the machine asleep partway) a scroll box can also come out 15px narrower, a
scrollbar's width, and Mordie may not reveal its solution in time, so a `solution` case is missing
its `[solution]` mark on one side. Both went away when rerun alone (GH-156). `::before` and
`::after` aren't compared.

The compare logic is unit-tested: `python3 -m unittest discover -s scripts/web/phone_fit/tests -t
scripts/web/phone_fit`.

## On a real phone

The user runs `! scripts/web/phone_fit/serve_lan.sh` (auto mode blocks the agent from serving to
the network); it prints the address to open on a phone on the same Wi-Fi, and a QR code for the
phone's camera when `qrencode` is installed (`brew install qrencode`), so the phone's browser can't
swap in an older address from its history (GH-148). Ctrl+C stops it.
