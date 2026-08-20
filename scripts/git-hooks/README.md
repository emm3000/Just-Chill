# git-hooks

Tracked git hooks for this repo. Not active after clone — opt in once per clone:

```bash
git config --local core.hooksPath scripts/git-hooks
```

`pre-push` runs `./gradlew --quiet --console=plain qualityGate` and nothing else. It blocks the push
on any finding not already in `config/detekt/baseline-*.xml`; skip it with `git push --no-verify`.

The gate is defined once, in
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt`, so this hook runs
exactly what CI runs. **Change the plugin, not the callers.** Which baseline file belongs to which
task: `docs/CODE_QUALITY.md`.
