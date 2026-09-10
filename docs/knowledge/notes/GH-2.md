# GH-2: Adopt yuriys-agentic-boyz multi-skill workflow

## Notes

- 2026-09-10: The framework needs a proper setup process for the ticket workflow itself. It should cover: (1) the ticket's structure (fields, required sections), (2) the ticket's possible states (Backlog/Brief/In Progress/In Review/In QA/Done per `docs/workflows/FEATURE.md`), (3) how those states/transitions map onto GitHub Issues specifically (labels vs. project board columns — GitHub Issues has no built-in state machine like Linear), and (4) as part of setup, teaching the user about the learning/knowledge flows (`brief`, `debrief`, `note`, `checkup`) so they know when and why to invoke each one, not just that they exist.
- 2026-09-10: `docs/MODEL_TIERS.md`'s skill capability map had drifted out of sync with the actual skill set (listed 10 skills, repo had 18) before this PR fixed it during `calibrate`. The setup process should have a step (or a `checkup`/`calibrate` check) that verifies the capability map matches `skills/*/*/SKILL.md` on disk, so this doesn't silently go stale again as skills are added or renamed.

## Debrief summary
