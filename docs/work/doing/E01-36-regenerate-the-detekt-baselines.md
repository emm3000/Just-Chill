# E01-36 — Regenerate the detekt baselines

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] every baseline in `config/detekt/` is rewritten by its own `detektBaseline*` task, run from a
      green tree at HEAD, and the commit carries exactly what those tasks wrote
- [ ] `baseline-data-main.xml` no longer names a single generated SQLDelight file — no
      `TransactionsQueries.kt`, `Recurring_movementsQueries.kt`, `CategoriesQueries.kt`,
      `AccountsQueries.kt` or `EmmDatabaseDataImpl.kt` entry survives
- [ ] the stem files `baseline-{androidApp,data,domain}.xml` are untouched — they belong to the
      plain `detekt` task, which is not a gate
- [ ] the gap between `baseline-androidApp-devDebug.xml` and `baseline-androidApp-prodDebug.xml` is
      either gone or the closing commit states why the two flavors legitimately differ
- [ ] the closing commit states the entry count per file, before and after
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`DetektConventionPlugin` excludes any path containing `/build/`, and SQLDelight generates into
`data/build/generated/sqldelight/` — so detekt has not analysed those files since `c94e2901`, but
their entries were never swept. Measured 2026-08-24: 529 of the 577 entries in
`baseline-data-main.xml` are dead. Stripping them and running
`./gradlew :data:detektMainAndroid --rerun-tasks` exits 0.

A regeneration at a green HEAD grandfathers nothing new by construction: with the gate passing there
are no unsuppressed violations left to capture. `docs/CODE_QUALITY.md` gotcha 1 notes the baseline
creates zero pressure on old code, and an inventory padded with dead entries cannot be read as a
to-do list at all.
