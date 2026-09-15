Route this invocation to a subagent. **Do not execute the skill yourself.**

**Skill:** audit
**Tier:** THOROUGH + TACTICAL
**Model alias:** opus

Steps:
1. Read `skills/verify/audit/SKILL.md` using the Read tool.
2. Spawn an Agent with:
   - `model`: `"opus"`
   - `prompt`: full content of the skill file, followed by the arguments below.
3. Before sending, confirm the `prompt` argument you're about to pass actually contains
   the skill file's full text (not a placeholder token) — re-read it back if unsure.

**Arguments:**
$ARGUMENTS
