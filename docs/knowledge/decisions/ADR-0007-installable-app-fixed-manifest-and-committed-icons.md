# ADR-0007 — install the game from a manifest whose identity is fixed for good, with icons rendered once from the palette and committed

## Status
`accepted`

## Context
GH-262 made the game play offline; GH-357 lets players install it (a home screen, a dock or an app list) with its own icon and window. Browsers decide that from a web app manifest, an icon set and a few meta tags. The site is static files on GitHub Pages under the sub-path `/mordovorot/` (GH-111), with no custom headers (`docs/CONSTRAINTS.md`), and the dev server runs at the root, so every URL must be relative.

## Decision
- **The manifest is `manifest.webmanifest`, with `start_url` and `scope` `"./"` and `id` `"mordovorot"`.** Browsers treat the `id` as the app's identity: changing it later makes every existing install a different app. The file name, the `id` and `./` are therefore fixed, like `offline-worker.js` (ADR-0006). `start_url` and `scope` resolve against the manifest's own URL, so `./` is right on the public site and on localhost. An `id` is parsed against the *origin* of `start_url`, not the manifest's URL, so `./` would be the whole `github.io` user origin, shared with every other app there (the audit of PR #358 found this); `mordovorot` makes it `<origin>/mordovorot`.
- **The icons are committed PNGs**, rendered by `scripts/web/icons/render_icons.js` (Node and Playwright's Chromium) from `palette.css`: the board in miniature (frame colour, 3x3 tiles in the board's own hues, the middle row shifted half a tile and wrapping). CI never runs the script; `AppIconsTest` reads the PNGs back and fails, naming the script, when a palette change was not followed by a re-render. This keeps the build free of a browser and the files byte-stable (the offline worker hashes them).
- **Variants:** 192 and 512 (`any`, rounded, transparent corners), a maskable 512 (frame colour full bleed, the board inside the 80% safe circle), `apple-touch-icon` 180 (opaque, full bleed, as iOS rounds it itself) and a 32 px favicon. No SVG favicon (uneven Safari support, one more file).
- **One name in every language** (`Mordovorot`), `display: standalone`, no `orientation` (the page works in landscape, GH-147).
- **One `theme_color` and `background_color`:** the light `--page`. The manifest can hold only one; while the page runs, two `theme-color` metas with `prefers-color-scheme` set the bar colour per theme.
- **iOS status bar `default`:** the page stays below the bar, so no `viewport-fit=cover` and no safe-area insets are needed.
- **The offline worker is unchanged.** It lists every file `jsProcessResources` outputs, so the manifest and `icons/` are cached automatically; `checkWebManifest` fails the distribution when the manifest isn't valid JSON or a file it or the page names isn't shipped (`checkOfflineManifest` already requires every shipped file to be listed).
- **Apple installs have their own storage.** An installed copy on iOS and macOS Safari starts with no saves (and shows the tutorial) and needs one online launch before it plays offline; a game moves across with a share link. Documented, not worked around.

## Alternatives considered
| Option | Why not chosen |
|---|---|
| Render the icons at build time | CI would need Chromium, and a render difference would change the hashes; the test already catches a stale render. |
| `manifest.json` | Works, but `.webmanifest` is the standard name and is served as `application/manifest+json`. |
| A dark `theme_color` or a middle colour | Light is the default theme; a middle colour matches neither. |
| `black-translucent` status bar | The page would draw under the bar and need `viewport-fit=cover` and `env(safe-area-inset-*)`: layout work outside this ticket. |
| `kotlinx-serialization-json` in the JVM tests | A new dependency needs its own work unit (GH-109); a regex test plus Gradle's bundled `JsonSlurper` in the build check parses the real JSON. |
| An in-page Install button | Needs `beforeinstallprompt`, which Safari lacks; the browsers' own offers are enough. |

## Related ticket
GH-357

## Date
2026-11-27
