---
name: plan
effort: THOROUGH
reasoning: ARCHITECTURAL
output_style: DETAILED
description: Produce a structured implementation plan for a PM issue or milestone before any code is written. Fetches the issue, reads the codebase, breaks the work into implementation units, posts the plan as a PM comment, and waits for user approval. For large changes spanning multiple files, new domain entities, new dependencies, or architectural shifts.
---

The project management tool is **GitHub Issues**. The issue identifier prefix is **GH**.

This skill produces plans, not code.

---

## Steps

### 1. Fetch the issue(s)

Retrieve the full details of each issue passed in (title, description, acceptance criteria, comments).

| Tool | How |
|---|---|
| Linear | `mcp__linear__get_issue` for each ID; if a milestone was named, `mcp__linear__get_milestone` then list issues filtered to it |
| Jira | `GET /rest/api/3/issue/{key}` |
| GitHub Issues | `gh issue view {number}` |
| GitLab | `glab issue view {number}` |

### 2. Read the codebase

Read `docs/ARCHITECTURE.md`, `docs/PRODUCT_SPEC.md`, and the source files most relevant to the work. Use search tools to locate existing models, repositories, view models, and UI files. Name real files and classes — do not invent hypothetical ones.

### 3. Produce the implementation plan

Use this format exactly. Omit a section entirely if it has no content.

@skills/design/plan/resources/plan-template.md

When the plan decides where a user-facing message goes (stderr, a status line, a dialog), check that the user can actually see it there while the active UI is running - e.g. a full-screen TUI hides stderr (GH-79).

Dependency and tool-version updates get a work unit of their own, never mixed into feature work. That unit's verification runs the full test suite plus any scenario/integration suite. Before bumping, search the build files for every place each version is written. If a version appears more than once, the same unit moves it into a single source, such as the `gradle/libs.versions.toml` version catalog (GH-109).

If no work unit changes observable behaviour (UI, text, analytics, save format), propose `Workflow: REFACTOR` (`docs/workflows/REFACTOR.md`) in the plan, so the WUs can run without user questions until the debrief. Put it at the top of the plan and ask about it as its own question when presenting the plan, never as one item in a list of technical decisions: it decides whether the user is consulted again before the final PR (GH-41: approved as "D7" without being noticed).

Before proposing a CHANGELOG tag, read `skills/manage/ship/resources/changelog-tags.md` - GH-78's plan claimed no refactor tag existed while `[app]` did.

Check the line count of every source file in "Files touched" against the `docs/CODE_STYLE.md` size guideline (~300 lines). For a file already over it, or one the plan's additions will push over, put the split in the plan: a cycle in this WU, or a follow-up ticket, chosen as a decision. Don't leave it for review to find (GH-160: `WebGame.kt` was at 320 lines when the plan named it, and the split came out of review).

### 4. Post the plan as a PM comment

Post the full plan text as a comment on the primary issue so `implement` can reference it.

| Tool | How |
|---|---|
| Linear | `mcp__linear__save_comment` on the issue |
| Jira | `POST /rest/api/3/issue/{key}/comment` |
| GitHub Issues | `gh issue comment {number} --body "..."` |
| GitLab | `glab issue note create {number} --message "..."` |

### 5. Present and wait

Show the plan to the user and wait for approval or adjustments. Do not proceed until the user explicitly approves.

Whoever talks to the user (the orchestrator, when this skill ran in a subagent) shows two
things in the session text itself, before asking anything (GH-149):
- a short summary of the plan: the approach, the files it touches, the tests and what's out of
  scope; the full text stays in the issue comment;
- every decision as its own item: what is being decided, each option with its trade-off, and
  the recommendation.

Only then ask. Each answer must be readable without the issue open: never fold several
decisions into one "accept all" option unless each was shown first.

### 6. Update ARCHITECTURE.md (after approval)

If the plan introduces new layers, directories, classes, or dependencies not already in `docs/ARCHITECTURE.md`, update that file now. Keep the existing structure — add to it, do not rewrite it.

---

## Constraints

- Do not write application code.
- Keep plans concrete: reference real files and classes from the current codebase.
- Never modify `CLAUDE.md` — that is the orchestrator's file.
