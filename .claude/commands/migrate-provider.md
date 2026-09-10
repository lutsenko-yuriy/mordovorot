Route this invocation to a subagent. **Do not execute the skill yourself.**

**Skill:** migrate-provider
**Tier:** RAPID + TACTICAL
**Model alias:** haiku

Steps:
1. Read `skills/configure/migrate-provider/SKILL.md` using the Read tool.
2. Spawn an Agent with:
   - `model`: `"haiku"`
   - `prompt`: full content of the skill file, followed by the arguments below. Pass any arguments after the command as the role/provider hint.

**Arguments:**
$ARGUMENTS
