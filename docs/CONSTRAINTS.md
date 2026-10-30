# Project Constraints

## Team & capacity

- Solo developer working with AI agents: agents implement, the developer reviews and approves

## Stage

- Pre-launch hobby prototype; optimise for clean architecture and experimentation, no users to protect

## Environment

- Local macOS terminal for manual play-testing; GitHub Actions ubuntu runner for build and test
- The user's main browser holds their own tabs: never open URLs in it or drive it (GH-110). Open
  PR/MR links in another browser and smoke-test web pages in a headless one with a throwaway
  profile. The commands for this machine are in `CLAUDE.local.md` (`## Browser`); without them,
  give the URL and don't open it.

## Screen readers

- Frozen until volunteers who use a screen reader test it (GH-171): what's shipped stays, untested
  beyond one sighted developer's VoiceOver runs. New work adds no screen-reader features and no
  VoiceOver checklists; keys and focus are still checked, headless and by hand.

## Budget & compliance

- Free tiers only, no paid services; analytics stay local/no-op and no personal data is collected
