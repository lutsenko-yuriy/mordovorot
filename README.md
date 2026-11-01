# Mordovorot

A version of Loopover, a sliding-row/column puzzle, written in Kotlin. It runs in a web browser, at
https://lutsenko-yuriy.github.io/mordovorot/.

The board is a `SQUARE_SIDE × SQUARE_SIDE` grid (3×3 to 9×9, default 4×4) holding a
shuffled permutation of numbers. Rows shift left/right and columns shift up/down,
cyclically wrapping around, until the board is back in ascending order.

## Credits

Mordovorot is based on Loopover, a puzzle created by Cary Huang (carykh). The original
is on [OpenProcessing](https://openprocessing.org/@carykh/580366), and Cary introduces it in
[this video](https://www.youtube.com/watch?v=95rtiz-V2zM). The community site,
[loopover.xyz](https://loopover.xyz), was made by
[Janis Pritzkau](https://github.com/janispritzkau). Mordovorot is an independent
reimplementation for the browser. It uses the same rules (cyclic row/column
shifts on a square board), but none of Loopover's code or assets.

## Running it

**Requires:** JDK 17. Gradle comes with the wrapper, so you don't need to install it.

### In a browser

Play it at **https://lutsenko-yuriy.github.io/mordovorot/**. Every green merge to `master` publishes the latest version
there (GitHub may serve the previous one for up to ~10 minutes).

To run it locally while working on it:

```bash
./gradlew :web:serve -t    # then open http://localhost:8080
```

Click the arrows, or drag a row or column; the keyboard works too. Saves are kept in the browser's `localStorage`, per address (the public site and
`localhost:8080` each have their own). With `-t` the server rebuilds when the code changes; reload the tab if it
doesn't reload by itself.

## Testing

```bash
./gradlew check                                                         # game, incl. shared/ modules
python3 -m unittest discover -s scripts/onboard/tests -t .              # onboarding tooling
python3 -m unittest discover -s scripts/skill_router/tests -t scripts  # skill router
```

Run the Python tests on Python 3.11+ for full coverage. Under 3.9 the `tomllib` tests are skipped.

## Architecture

The game uses MVVM (Model-View-ViewModel):

- `board_model`: game state and rules.
- `viewmodel`: mediates between view and model.
- `web/`: the browser client (the view).
- `storage` and `analytics`: supporting services.

See `docs/ARCHITECTURE.md` for the full layout and the layer rules.

## Working on this project with Claude Code

This repo uses the
[agentic-boyz](https://github.com/lutsenko-yuriy/yuriys-agentic-boyz)
multi-skill workflow. `AGENTS.md`, included via `CLAUDE.md`, is the
orchestrator. It points to the skills under `skills/` and to the project facts in `docs/`.
Issues and backlog live in [GitHub Issues](https://github.com/lutsenko-yuriy/mordovorot/issues).

**Requires:** `git`, `python3`, a Python 3.11+ interpreter, and an authenticated `gh` (`gh auth login`).
The gate runs on any `python3`, 3.9 included. Without `python3`, the gate denies every tool call, and `/onboard` can't fix that.
Onboarding's `check`, `apply` and `mark` need 3.11+ (for example `python3.12`). `/onboard` finds one on its own.

1. Open Claude Code **from the repo root**. Claude Code reads `.claude/settings.json` only
   from the directory it starts in. If you start it from a subdirectory, the gate hooks
   don't load.
2. Run `/onboard`. Until it finishes, a gate blocks most writes, other skills, subagents
   and shell commands. The project is already configured, so here it gives a short
   orientation and then writes a per-clone marker inside the git dir. The marker is never
   committed, so each collaborator runs `/onboard` once.
3. Start with `/summarize`.

If you change the configuration, commit and push it, so collaborators don't work from a stale one.
