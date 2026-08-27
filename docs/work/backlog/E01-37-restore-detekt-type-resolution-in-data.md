# E01-37 — Restore detekt type resolution in `:data`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `:data:detektDebug --rerun-tasks` reports no `compiler errors found during analysis` line.
      It reports **472** today; `:presentation`, `:ui-android` and `:androidApp` report none, so the
      cause is specific to this module.
- [ ] The seven `@file:Suppress("RedundantSuspendModifier")` in `:data`'s `*LocalDataSource.kt` are
      deleted and detekt stays green — they exist only because the broken analysis cannot see that
      `withContext` suspends.
- [ ] Every detekt rule that needs type resolution is shown either firing on `:data` or provably
      clean; that list, not the error count, is what this ticket is about.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass.

## Context

`DetektConventionPlugin.excludeGeneratedSources()` drops SQLDelight output from detekt's
*resolution* scope as well as its *analysis* scope, so hand-written code referencing
`TransactionsQueries` is analysed against unresolved symbols. Its task exposes `classpath`
separately from `source` — putting the generated roots on the former only would resolve without
linting, if the alpha honours the split.

E11-02 cleared the nine errors this ticket opened on (they were `expect` declarations). E11-05's
conversion to a plain Android library raised the count to 472 and manufactured 26 false positives
with it, so the original hypothesis is back, larger.
