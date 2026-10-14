# Bookmarks

Hand-authored vocabulary of bookmarks used across `docs/knowledge/notes/`. Each note's
`bookmarks:` frontmatter must use only names from this table (lowercase kebab-case). Add
a new row when a genuinely new theme emerges from the corpus — don't invent a synonym for
something already covered here.

`scripts/notes/index.py --check` fails a note that uses a bookmark not listed below.

Starts empty on a fresh project. Add the first row the first time `/debrief` or `/note`
surfaces a recurring theme worth tagging; re-validated quarterly as part of the heavy
`/checkup` tier (dimension 9, "Knowledge-corpus bookmark health").

| Bookmark | Meaning |
|---|---|
| `token-cost` | Token/budget burn, or review/audit cost out of proportion to what it found |
| `subagent-routing` | A routed skill's subagent didn't spawn, or spawned with the wrong/placeholder prompt |
| `template-drift` | Gaps or staleness inherited from the upstream workflow template, or generated artifacts drifting from their sources |
| `scope-growth` | A ticket's planned work units grew during implementation |
| `smoke-test-gap` | A smoke test missed or misattributed a real problem |
| `workflow-order` | Workflow steps run in the wrong order (e.g. debrief after merge) |
