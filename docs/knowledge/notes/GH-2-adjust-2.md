---
bookmarks: []
---

# GH-2-adjust-2: Adopt yuriys-agentic-boyz multi-skill workflow — follow-up

## Notes

### 2026-09-10

**Analytics setup was never addressed during framework adoption**

While briefing/analyzing GH-6 (save/load), we hit `docs/ANALYTICS_EVENTS.md` for the
first time since GH-2 adopted the framework — and discovered there's no analytics
service abstraction anywhere in the codebase at all, not even a stub. GH-2's adoption
work carried over `analyze`/`docs/ANALYTICS_EVENTS.md` from the template but never asked
the basic setup question: does this project have (or want) an analytics SDK, and if not,
should adoption scaffold a no-op abstraction so the `analyze` skill's output has
somewhere real to land? We ended up doing that reactively on GH-6
(`analytics.AnalyticsService` / `analytics.NoopAnalyticsService`) instead of it being
part of initial setup.

Worth adding to the framework's setup checklist alongside the licensing gap already
noted in [[GH-2-adjust-1]]: during adoption, explicitly ask whether the project has an
analytics service, and if not, scaffold a no-op implementation so `analyze` isn't
producing plans with no code to attach to until the first feature that happens to need it.

## Debrief summary
