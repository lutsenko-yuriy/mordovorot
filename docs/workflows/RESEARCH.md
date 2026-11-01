# Research Workflow

Use this workflow for research-only tickets (no production code, no PR beyond the debrief commit; a prototype worth keeping goes to the follow-up ticket as its starting point, not into this one). For features use `docs/workflows/FEATURE.md`; for bugs/CI use `docs/workflows/TROUBLESHOOT.md`.

@skills/shared/decision-guidelines.md

## Steps

1. **Scope the ticket before surveying.** If this scoping/survey work is expected to span multiple sessions/days, move the ticket to **Brief** first and back to Backlog/Todo once scoped. Two passes, both ticket-specific (not covered by the general `docs/CONSTRAINTS.md`):
   - **Ticket-specific constraints.** List constraints unique to this ticket — including non-functional ones (existing infra, timing, ownership) — beyond the standing project constraints. Surfacing these early can invalidate the premise of the research before survey effort is spent.
   - **MoSCoW the requirements.** Sort the ticket's research goals into Must/Should/Could/Won't. Survey and evaluate Musts first — a goal that turns out to be Could-have may already be satisfied by the status quo.
   - **One kind of question per ticket.** If the ticket mixes an algorithmic question (which method works) with an engineering one (where and when it runs, how it ships), split it before surveying, so each gets its own survey and measurements. GH-116 carried both and was split during its brief (GH-211).

2. **Survey existing alternatives first.** Before forming opinions, document what comparable projects or tools already do. This grounds proposals in real precedent and surfaces patterns that are otherwise easy to miss. Cite a working link to the source for each claim — an assertion without a link can't be checked and risks being a hallucination. For a claim or topic that suits a literature search, invoke the `research` skill (`/research`) to produce this survey directly — it is not mandatory for every research ticket (e.g. surveying internal code precedent doesn't need it).

   When the survey has no numbers for the candidates, measure them. Write the prototypes in the project's own stack (the language, frameworks and libraries named in `docs/TECH_STACK.md`), not a stand-in: if the stack names RxJava, prototype with RxJava, not Kotlin Flows. Keep them as throwaway code outside the production source sets (a scratch test run through Gradle). The timings then carry over, and a prototype that wins is a ready starting point. GH-116 measured in Python, so its timings were only orders of magnitude and the 10 ms slice fit stayed unconfirmed.

3. **Evaluate trade-offs against `docs/CONSTRAINTS.md`.** For each option, assess how well it fits the standing constraints, rank options by fit, and call out explicitly what could go wrong with each given current constraints (e.g. "requires ongoing human review — unsustainable for a small team").

4. **Capture findings mid-session** with `/note GH-XX: <observation>` as conclusions emerge — do not batch everything to the end.

5. **If the research concludes in a standing decision** — one that should be discoverable without knowing which ticket produced it — write an `ADR-NNNN` in `docs/knowledge/decisions/` (see `docs/knowledge/decisions/README.md`) before debriefing. Day-to-day findings stay in the ticket's own notes file; the ADR is only for the decision itself.

6. **Debrief and close** with `/debrief GH-XX` when all research questions are answered.
