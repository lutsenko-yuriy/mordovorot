Route this invocation to a subagent. **Do not execute the skill yourself.**

**Skill:** summarize
**Tier:** RAPID + MECHANICAL
**Model alias:** haiku

Steps:
1. Read `skills/manage/summarize/SKILL.md` using the Read tool.
2. Spawn an Agent with:
   - `model`: `"haiku"`
   - `prompt`: full content of the skill file.

<!-- no arguments — summarize takes none -->
