# git-hooks

Tracked git hooks for this repo. Not active by default after clone — opt in with:

```bash
git config --local core.hooksPath scripts/git-hooks
```

Run once per clone.

## Hooks

| Hook | What it does | Skip with |
|---|---|---|
| `pre-push` | Runs `./gradlew qualityGate` — detekt over every source set that holds code, the JVM host test suites, dev lint, and (on macOS) the iOS compile. Blocks the push on any new finding, one not in `config/detekt/baseline-*.xml`. | `git push --no-verify` |

The hook runs exactly one command:

```bash
./gradlew --quiet --console=plain qualityGate
```

It used to hold its own list of five detekt tasks and no tests at all. That list drifted from the
three GitHub workflows and from `docs/WORKFLOW.md`, and none of the four was a superset of
the others. The gate is now defined once, in
`build-logic/src/main/kotlin/com/emm/buildlogic/QualityGateConventionPlugin.kt`, and this hook runs
exactly what CI runs. **Change the plugin, not the callers** — that is the whole point of there
being one definition. The task list it expands to is documented in the plugin's KDoc.

## Why not plain `./gradlew detekt`

That is what the hook used to run, and after the KMP migration it stopped meaning
anything:

```
> Task :detekt            NO-SOURCE
> Task :data:detekt       NO-SOURCE
> Task :ui-android:detekt NO-SOURCE
> Task :domain:detekt     NO-SOURCE
> Task :androidApp:detekt           ← the only one that ran
```

detekt's plain task does not see KMP source sets, so the gate silently shrank to
`:androidApp` — nine files, none of them the UI, ViewModels or Koin wiring that
now live in `:ui-android`.

The plain `detekt` task still exists and still works; it just is not a gate. Its
stem baselines (`config/detekt/baseline-<module>.xml`, no source-set suffix)
belong to it and are now unused by the hook. They are kept rather than deleted so
a manual `./gradlew detekt` keeps behaving as before.

Baseline scheme (which file belongs to which task) is documented in `docs/CODE_QUALITY.md`.
