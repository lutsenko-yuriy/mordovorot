---
bookmarks: []
---

# GH-2: Adopt yuriys-agentic-boyz multi-skill workflow

## Notes

<!-- Each entry: dated heading, then named observations (bold title + paragraph).
     If the ticket is reopened after this file is finalised, create GH-XX-adjust-N.md instead of editing this file. -->

### 2026-09-10

**Setup process needs to cover the ticket workflow itself**

The framework needs a proper setup process for the ticket workflow. It should cover: (1) the ticket's structure (fields, required sections), (2) the ticket's possible states (Backlog/Brief/In Progress/In Review/In QA/Done per `docs/workflows/FEATURE.md`), (3) how those states/transitions map onto GitHub Issues specifically (labels vs. project board columns — GitHub Issues has no built-in state machine like Linear), and (4) as part of setup, teaching the user about the learning/knowledge flows (`brief`, `debrief`, `note`, `checkup`) so they know when and why to invoke each one, not just that they exist.

**`docs/MODEL_TIERS.md` skill capability map had drifted**

It listed 10 skills while the repo had 18, before this PR fixed it during `calibrate`. The setup process should have a step (or a `checkup`/`calibrate` check) that verifies the capability map matches `skills/*/*/SKILL.md` on disk, so this doesn't silently go stale again as skills are added or renamed.

**Review + audit findings from PR #1 (framework adoption)**

Full `review` (architectural) and `audit` (runtime/migration) passes ran against PR #1. Summary, with resolution:

- 🔴 *Fixed in PR #1:* Our own first note failed the framework's own validator — `scripts/notes/index.py --check` exited 1 (`GH-2.md` missing `bookmarks:` key, `INDEX.md` stale).
- 🟡 *Fixed in PR #1:* `skills/verify/draft-scenarios/SKILL.md` hardcoded `context: linear`, a leftover template placeholder that resolves to a real `LinearProvider` at runtime (`scripts/skill_router/app.py`'s `_make_provider` treats unrecognised keys as literal provider names) despite this project having no PM role configured — risk of leaking another workspace's issues as context if a stray `LINEAR_API_KEY` is ever present.
- 🟡 *Fixed in PR #1:* `AGENTS.md` claimed "CI handles build numbers automatically — do not touch", false and self-contradicting (`project-config.md` says "Manual — no CI is configured yet"; no `.github/workflows/` exists).
- 🟡 *Fixed in PR #1:* `.gitignore`'s opt-out comment referenced `.claude/agents/`, which doesn't exist in this template version — the real directory is `.claude/commands/`.
- 🟡 *Deferred, tracked here:* `skills/manage/ship/SKILL.md` hunts for a version file (`pubspec.yaml`/`package.json`/etc.) that doesn't exist in this repo — would half-complete a ship (close issues, then fail the version-bump step). Needs either a real version file (once a build tool is chosen) or a documented no-op path in `ship` for version-file-less projects.
- 🟡 *Deferred, tracked here:* `skills/shared/project-config.md` documents no test harness, but `draft-scenarios`/`implement` are unconditionally wired into `docs/workflows/FEATURE.md` and expect one. Blocked on choosing a build tool + test framework for this repo (see GH-2 note above re: `docs/ARCHITECTURE.md`'s "no build tool yet" gap).
- 🟡 *Documented, not code:* `docs/MODEL_TIERS.md`'s Active mapping makes `scripts/skill_router` inert for all six tiers — it only resolves a literal `lm-studio` alias, never `opus`/`sonnet`/`haiku`. Intentional (we route via Agent-spawn/passthrough, not the script), but wasn't spelled out; left as a one-line PR-comment clarification rather than a doc rewrite.
- 🟡 *Documented, not code:* `skills/configure/calibrate/resources/stub-formats.md` only spells out the "spawn up" and "same alias" routing cases, not the "spawn down" case this project actually uses (RAPID tier → `haiku`, cheaper than session `sonnet`). Works correctly today; the doc's routing-rule bullet list should eventually be widened to name the spawn-down case explicitly.
- 🟢 *Deferred, tracked here:* `docs/workflows/RESEARCH.md` and ADR-0002 reference `docs/CONSTRAINTS.md`, which the template never creates.
- 🟢 *Deferred, tracked here:* `docs/BACKLOG.md`/`docs/CHANGELOG.md` attribute regeneration to a nonexistent "product-owner-merge" skill / "Product Owner agent" — the real owner is `ship`.

## Debrief summary

### 2026-09-10

**What went well**
- The adoption landed quickly and cleanly despite the number of gaps found — the scaffolding itself is sound, and the mismatches were all shallow/mechanical rather than structural.
- The `review` + `audit` loop worked as intended: it surfaced real, previously-invisible gaps (the notes validator failure, the stale Linear context, the false CI claim) that a straight read-through likely would have missed.

**What was hard or surprising**
- A bunch of the gaps found were things that should have been retrofitted specifically to this project (version file, test harness, PM provider assumptions) rather than genuine bugs — the cost of the upstream template trying to stay universal across stacks/PM tools.

**What to change**
- Adapt `yuriys-agentic-boyz` (the upstream template) itself to be more generic/adaptable, so future adoptions hit less of this per-project friction. Out of scope for this repo's own files — tracked as feedback for the upstream template, not a local fix.
