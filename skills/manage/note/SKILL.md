---
name: note
effort: RAPID
reasoning: MECHANICAL
output_style: CONCISE
description: Capture a quick observation or decision into the project knowledge base mid-session. Infers the active ticket from session context (confirms before writing), appends a timestamped bullet to docs/knowledge/notes/GH-XX.md, and creates the file if it does not yet exist. When the user says they are stopping ("Okay, we'll stop here") or passes `stop`, it instead replaces the ticket's `## Where we stopped` section, so the next session can pick up.
---

@skills/shared/project-config.md

This skill writes one entry, then stops. It does not open a dialog.

---

## Steps

### 1. Resolve the ticket tag

**If the argument starts with `GH-XX:` (explicit tag):** use that ticket ID. Strip the `GH-XX:` prefix to get the note text.

**If no explicit tag:** check the current session context for an active ticket (e.g. a ticket currently In Progress, or one mentioned in the recent conversation).

- If a ticket is found: ask once — *"This note will be tagged to GH-XX — correct?"* Wait for confirmation before writing.
  - If the user confirms: proceed with that ticket.
  - If the user corrects it (supplies a different ticket or label): use the corrected value.
- If no ticket is found: ask the user — *"Which ticket should this note be tagged to?"* Use the answer; do not guess.

One question maximum. Do not write until the ticket is resolved.

### 2. Resolve the note text

The note text is everything after the `GH-XX:` prefix in the argument, or the full argument if no explicit tag was given.

If the argument is empty after stripping the tag (or no argument was given at all): ask — *"What's the note?"* — and use the answer.

### 3. Write the entry

Resolve the target file: `docs/knowledge/notes/GH-XX.md` (using the ticket ID from step 1).

**If the file does not exist:** create it with this structure:

```markdown
---
bookmarks: []
---

# GH-XX: <ticket title>

## Notes

## Debrief summary
```

**Append** a timestamped bullet at the end of the `## Notes` section (after all existing bullets, or as the first line if there are none yet):

```
- YYYY-MM-DD: <note text>
```

Use today's date in `YYYY-MM-DD` format.

If the file was new, `git add` it, then run `python3 scripts/notes/index.py && python3 scripts/notes/index.py --check`. CI rejects a note without a `bookmarks:` key or a stale `INDEX.md`, and the index lists git-tracked notes only (GH-103).

### 4. Where we stopped (stop mode)

Instead of steps 2 and 3 when the user says they are stopping ("Okay, we'll stop here" and the like) or the argument is `stop` (optionally `GH-XX: stop`); step 1 still resolves the ticket.

Write the `## Where we stopped` section of the ticket's notes file, between `## Notes` and `## Debrief summary` (create the file as in step 3 if needed). If the section exists, **replace** it: it is one block, never a log.

```markdown
## Where we stopped

YYYY-MM-DD, <branch> (PR #N, CI <green|red|pending>)

- Done: <what is finished and merged or pushed>
- Open: <what is unfinished, and who it waits on: the user, CI, a review>
- Next: <the first step the next session takes>
```

Take the facts from `git status`, `git log`, `gh pr view` and the conversation; do not ask the user for them. Then go to step 5.

### 5. Report back

One line: *"Noted in `docs/knowledge/notes/GH-XX.md`."*

---

## Constraints

- Never write to `## Debrief summary` — that section belongs to `/debrief`. In stop mode the only existing text you may change is the `## Where we stopped` section.
- One question maximum across all steps. If both the ticket and the note text are unknown, ask for the ticket first; ask for the note text in the same response only if the ticket was explicit in the argument.
- Never modify production code, tests, or any file outside `docs/knowledge/notes/`.
