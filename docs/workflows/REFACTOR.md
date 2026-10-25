# Refactor Workflow

An extension of `docs/workflows/MULTI_WU.md` for tickets whose every WU preserves behaviour
(class splits, dependency rewiring, test-infrastructure refactors). It uses the same branches,
PRs, `[wip]` tags and review skills as MULTI_WU. The difference is who decides that a WU is
done: once the plan is approved, the agent does (GH-78).

## When it applies

`plan` proposes `Workflow: REFACTOR` when no WU changes observable behaviour (UI, text,
analytics, save format). The user accepts it together with the plan. If a WU turns out to need
a behaviour change, that WU goes back to `MULTI_WU.md`.

## Autonomy window

From plan approval to the final WU's PR, the agent does not ask the user anything. For each WU:

1. Implement on `feature/GH-XX-WUN-<short>`, branched from a fresh `origin/main`. Move code
   verbatim and keep every existing assertion.
2. Get `./gradlew build` green, then smoke-run the real binary over the flows the WU moved.
   For the web client's CSS, also run `scripts/web/harness/styles_diff.sh` against master: it
   should report no differences (GH-156).
3. Open the PR and run `review` + `audit`. Fix every comment, suggestions included, and
   re-run until a round posts **no** findings.
4. Wait until `gh pr checks` reports every check finished and passed (exit 0, none pending).
5. Ship it as an intermediate WU: a `[wip]` entry, a patch bump and a squash-merge. No
   `/debrief`.

Nobody watches the WUs in this window, so check on routed subagents that run long: if one
hasn't reported back within ~10 minutes, check what it should have changed by now (commits,
PR state, checks). If it is stuck, stop it, finish the step yourself and tell the user
(GH-103).

## Stop and ask only when

- a WU needs a behaviour change, or the plan turns out to be wrong;
- CI stays red after a fix attempt and the cause isn't clear;
- a tool or permission is denied. Report it; don't work around it.

## Final WU

Run the plan's verification checklist. Automate the [human] items wherever a script can drive
the real app, and list what is left for the user. Then stop: the first question to the user is
the PR approval and `/debrief` (`FEATURE.md` step 12), followed by `/ship`.
