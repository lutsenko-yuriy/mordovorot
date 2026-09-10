---
bookmarks: []
---

# GH-6: Save and load game state to/from local files

## Notes

### 2026-09-10

**Review/audit comments (inline and summary) are too wordy**

User feedback during WU1's review loop: PR comments from `review`/`audit` (both inline
findings and the structured summary) were too verbose - full paragraphs of context and
reasoning per finding. Both skills' frontmatter already declares `output_style: CONCISE`,
but the actual output didn't read that way in practice. Applying a tighter style going
forward: lead with the fix/verdict, keep justification to a short clause, drop restated
context the diff/commit already carries. Worth revisiting the `review`/`audit` skill
files (or the `CONCISE` output style definition) to make this the default rather than
something that has to be caught per-session.

**MULTI_WU.md doesn't say where an intermediate WU's `[wip]` CHANGELOG entry goes**

`ship`'s WU1 pass (delegated to a haiku subagent, per the routing rules) inserted the
`[wip]` GH-6 bullet into the *already-released* `0.1.1` section (PR #5's entry) - my own
orchestrating instructions to the subagent said "append ... under the existing
unreleased/latest section context," which was ambiguous since no unreleased section
existed. Fixed by introducing a `## [Unreleased]` section instead, which subsequent
intermediate WUs append `[wip]` bullets to; the final WU's `ship` pass should convert it
into the real `## [X.Y.Z] — date` entry. `docs/workflows/MULTI_WU.md`'s "CHANGELOG tags
for intermediate WUs" section should say this explicitly (use/create an `## [Unreleased]`
header) rather than leaving the target section implicit - a small gap, but it produced a
genuinely wrong-looking changelog (an already-shipped release appearing to contain
unfinished work) on the very first intermediate WU this project has shipped.

**WU4 audit: win-condition gap with restored saves**

WU4 audit (PR #14, round 2) surfaced that restoring an already-solved save at the startup
prompt exits `play()` instantly with no win/board message shown — `board.isCorrect()` is
true right after `restoreState()`, so the while loop in `play()` never runs. This is an
instance of the pre-existing "no documented win-condition message" gap already listed in
`docs/PRODUCT_SPEC.md`'s Known gaps section, newly reachable via the startup restore flow.
Not fixed as part of GH-6 (out of scope per FEATURE.md's scope-expansion guidance) — worth
a small follow-up ticket for a win-condition message shown whenever `isCorrect()` becomes
true (including immediately after any load, not just at startup).
