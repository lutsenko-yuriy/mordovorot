# Mordovorot

A console prototype of a sliding-row/column puzzle board game, written in Kotlin.

The board is a `SQUARE_SIDE × SQUARE_SIDE` grid (default 4×4) holding a shuffled
permutation of numbers. Rows shift left/right and columns shift up/down,
cyclically wrapping around, until the board is back in ascending order.

## Running it

Built with Gradle (via the wrapper, no local Gradle install needed):

```bash
./gradlew run
```

## Testing

```bash
./gradlew test
```

`board_model` and `presenter` have full unit test coverage, independent of any
view/UI implementation. The `view` layer isn't covered yet — tracked separately.

## Architecture

MVP (Model-View-Presenter): `board_model` (game state + rules), `presenter`
(mediates view ↔ model), `view` (console I/O). See `docs/ARCHITECTURE.md`
for the full layout and layer rules, and `docs/PRODUCT_SPEC.md` for the
feature list.

## Working on this project with Claude Code

This repo is set up with the
[agentic-boyz](https://github.com/lutsenko-yuriy/yuriys-agentic-boyz)
multi-skill workflow. `AGENTS.md` (included via `CLAUDE.md`) is the
orchestrator; it points to skills under `skills/` for planning, TDD
implementation, review, and shipping, and to project facts in `docs/`.
Issues and backlog live in [GitHub Issues](https://github.com/lutsenko-yuriy/mordovorot/issues).

Start a session by asking Claude to follow the session-start steps in
`AGENTS.md`, or invoke the `summarize` skill directly.
