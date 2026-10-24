# Phone fit (GH-147)

Checks that the web game fits phone screens, headless, and serves it to a real phone.

## Headless measurements

```
./gradlew :web:jsBrowserDistribution
scripts/web/phone_fit/measure.sh "v=mouse" "v=touch&mode=solution&boards=3"
```

One line per case: `OK`/`BAD`, then the page and window sizes, the toolbar's height, width and
position, the footer's height, the tiles' position and size, Mordie's spot and the furthest any
control goes past the window's edge. `OK` means the page is exactly the window's size, nothing is
past an edge, the toolbar has at most `rows` rows and the footer one line.

Query options (harness.html):

| Option | Default | Meaning |
|---|---|---|
| `v` | `mouse` | `mouse`, `touch` (the `pointer: coarse` rules always on); `master`, `master-touch` with `--baseline` |
| `sizes` | SE through Pro Max, portrait | comma-separated `WxH` |
| `boards` | `3,4,5` | board sides |
| `mode` | `play` | `play`, `dialog` (Tile labels open) or `solution` (shifts until Give Up lights, then gives up) |
| `shorter` | `0` | px taken off each height, standing in for the browser's own toolbars (try `100`) |
| `rows` | `1` | toolbar rows allowed (`2` under 300px wide) |

To compare against master, build master's bundle into another directory and pass it with
`--baseline <dir>`; positions in the `master` lines should match the `mouse` ones where nothing
was meant to change. Keep each query to a few cases on the solution screen: Mordie needs a few
seconds each, and a query gives up after 10 minutes (`TIMEOUT`).

## On a real phone

The user runs `! scripts/web/phone_fit/serve_lan.sh` (auto mode blocks the agent from serving to
the network); it prints the address to open on a phone on the same Wi-Fi. Ctrl+C stops it.
