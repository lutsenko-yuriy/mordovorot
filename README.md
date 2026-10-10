# Mordovorot

A console prototype of a sliding-row/column puzzle board game, written in Kotlin.

The board is a `SQUARE_SIDE × SQUARE_SIDE` grid (3×3 to 5×5, default 4×4) holding a
shuffled permutation of numbers. Rows shift left/right and columns shift up/down,
cyclically wrapping around, until the board is back in ascending order.

## Running it

**Requires:** JDK 17. Gradle comes with the wrapper, so you don't need to install it.

The mouse and keyboard TUIs need a real terminal, so launch them through the
`installDist` launcher:

```bash
./gradlew installDist
./build/install/mordovorot/bin/mordovorot              # mouse TUI
./build/install/mordovorot/bin/mordovorot --keyboard   # keyboard-only TUI
```

`./gradlew run` always runs the line-based console mode, because Gradle doesn't pass
a terminal through to the game.

Flags:

- `--console`: force console mode. It also takes over whenever there's no interactive terminal.
- `--size=N`: start at board side `N` (3-5). This skips the startup restore/size prompts,
  which is useful for piped input.

Saves go to `saves/`, relative to the directory you launch from. See
`docs/PRODUCT_SPEC.md` for the full command list and behaviour.

## Testing

```bash
./gradlew test                                              # game
python3 -m unittest discover -s scripts/onboard/tests -t .  # onboarding tooling
```

## Architecture

The game uses MVVM (Model-View-ViewModel):

- `board_model`: game state and rules.
- `viewmodel`: mediates between view and model.
- `view`: console and TUI I/O.
- `storage` and `analytics`: supporting services.

See `docs/ARCHITECTURE.md` for the full layout and the layer rules.

## Working on this project with Claude Code

This repo uses the
[agentic-boyz](https://github.com/lutsenko-yuriy/yuriys-agentic-boyz)
multi-skill workflow. `AGENTS.md`, included via `CLAUDE.md`, is the
orchestrator. It points to the skills under `skills/` and to the project facts in `docs/`.
Issues and backlog live in [GitHub Issues](https://github.com/lutsenko-yuriy/mordovorot/issues).

**Requires:** `git`, `python3` 3.11+, and an authenticated `gh` (`gh auth login`).
Without `python3`, the onboarding gate denies every tool call, and `/onboard` can't fix that.

1. Open Claude Code **from the repo root**. Claude Code reads `.claude/settings.json` only
   from the directory it starts in. If you start it from a subdirectory, the gate hooks
   don't load.
2. Run `/onboard`. Until it finishes, a gate blocks most writes, other skills, subagents
   and shell commands. The project is already configured, so here it gives a short
   orientation and then writes a per-clone marker inside the git dir. The marker is never
   committed, so each collaborator runs `/onboard` once.
3. Start with `/summarize`.

If you re-run the full configuration, commit and push its output before collaborators
clone. Otherwise each of their `/onboard` runs repeats it.
