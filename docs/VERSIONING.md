# Versioning

<!-- Describe your versioning strategy. Example below — adapt to your project. -->

This project follows [Semantic Versioning](https://semver.org/): `MAJOR.MINOR.PATCH`.

**Major** — breaking changes (incompatible data formats, dropped platform support).
**Minor** — new features added in a backwards-compatible manner.
**Patch** — backwards-compatible bug fixes.

Version bumps are manual and require user approval before any change.

## CI/CD

`.github/workflows/build.yml` runs `./gradlew build :web:jsBrowserDistribution` (compile + test + assemble) and
`scripts/notes/index.py --check` (notes `INDEX.md` is current) on every push to `master`
and on every pull request. After a green build on a `master` push, its `deploy` job
publishes the web bundle (`web/build/dist/js/productionExecutable/`) to GitHub Pages
(GH-111); the web footer shows the version, so a bump is visible on the site. It does not
bump versions — that remains manual.
