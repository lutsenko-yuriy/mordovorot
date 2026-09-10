# Versioning

<!-- Describe your versioning strategy. Example below — adapt to your project. -->

This project follows [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`.

**Major** — breaking changes (incompatible data formats, dropped platform support).
**Minor** — new features added in a backwards-compatible manner.
**Patch** — backwards-compatible bug fixes.

Version bumps are manual and require user approval before any change.

## CI/CD

`.github/workflows/build.yml` runs `./gradlew build` (compile + test + assemble) on every
push to `master` and on every pull request. It does not publish artifacts or bump versions —
those remain manual.
