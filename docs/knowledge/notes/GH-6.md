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
