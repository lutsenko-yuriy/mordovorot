# ADR-0008 — stay an installable PWA and list the game in no app store for now

## Status
`accepted`

## Context
GH-357 made the game installable from the browser and GH-360 added an `Install the game` entry to the footer. GH-359 asked whether to publish it in Google Play and the Apple App Store as well. The standing constraints are a pre-launch hobby prototype with no users to protect, free tiers only and no paid services, a static GitHub Pages site on the sub-path `/mordovorot/` (`docs/CONSTRAINTS.md`, ADR-0007), and a solo developer. Findings were checked against official pages on 2026-11-27.

## Decision
- **No store listing for now: the game stays an installable PWA.** The owner also declined cheaper channels (an itch.io listing) for now; the rest is decided later.
- **Google Play needs two exceptions to "free tiers only" and a change of origin:**
  - a one-time US$25 fee ([Play Console help](https://support.google.com/googleplay/android-developer/answer/6112435));
  - for a new personal account, a closed test with at least 12 opted-in testers for 14 days before production access ([Play help](https://support.google.com/googleplay/android-developer/answer/14151465));
  - ID, a credit card and a verified Android device ([Play Console help](https://support.google.com/googleplay/android-developer/answer/6112435));
  - the Digital Asset Links file "at the root of the domain" ([Chrome docs](https://developer.chrome.com/docs/android/trusted-web-activity/integration-guide), [PWABuilder FAQ](https://docs.pwabuilder.com/builder/asset-links-faq.md)). For a project page under `lutsenko-yuriy.github.io/mordovorot/` that file would have to be served by the separate `lutsenko-yuriy.github.io` repository, or the game would move to a custom domain (about US$10 to 15 a year, estimated, not sourced). Either way the origin changes: `localStorage` saves are lost and the manifest `id` (ADR-0007) is tied to the origin.
- **The Apple App Store fits worst:** US$99 a year with no waiver for individuals ([Apple Developer Program](https://developer.apple.com/programs/whats-included/)), and guideline 4.2 requires "features, content, and UI that elevate it beyond a repackaged website" ([App Review Guidelines](https://developer.apple.com/app-store/review/guidelines/)). Developers report wrapper apps rejected under 4.2 even with notifications and sharing ([forum thread](https://developer.apple.com/forums/thread/704430)); no firsthand approval of a pure web-game wrapper was found. A bundled offline build (guideline 2.5.2) would be one more release channel.
- **The Microsoft Store is the one store that fits the constraints:** individual registration is free ([Microsoft Learn](https://learn.microsoft.com/en-us/windows/apps/publish/partner-center/open-a-developer-account)), and a PWA is packaged with PWABuilder ([Edge docs](https://learn.microsoft.com/en-us/microsoft-edge/progressive-web-apps/how-to/microsoft-store)). Not taken now (Windows-only audience); whether a sub-path `start_url` on a shared host is accepted is unverified.
- **Any future wrapper starts with save export/import.** Every copy (browser tab, Home Screen app, store wrapper) keeps its own storage, so a game moves between them by share link only (ADR-0005) until a file export and import exists.
- **Revisit when** there are real users asking for a store, or the budget constraint changes, or the origin is moved for another reason. Decide the domain before the game has many saves, since moving the origin loses them.

## Alternatives considered
| Option | Why not chosen |
|---|---|
| Google Play Trusted Web Activity | A fee exception, a closed test with 12 testers over 14 days, and an origin move (custom domain or the user-site repository), with the saves lost; about 6 to 12 hours of work (estimate). |
| Apple App Store wrapper | US$99 a year, a real risk of repeated 4.2 rejections, a bundled build to maintain, and its own storage; about 10 to 20 hours (estimate). |
| Microsoft Store through PWABuilder | Free and cheap, but a Windows-only audience and the sub-path question is untested; left for later. |
| itch.io HTML5 upload | HTML5 uploads with a seller-set revenue share of 0% to 100% ([itch.io docs](https://itch.io/docs/creators/payments)), but a second copy of the site with its own storage and updates; the owner declined it for now. |
| Samsung Galaxy Store, Amazon Appstore, Huawei AppGallery | PWA acceptance and terms could not be verified (hosts blocked or pages dated). |

## Related ticket
GH-359 (notes: `docs/knowledge/notes/GH-359.md`)

## Date
2026-11-27
