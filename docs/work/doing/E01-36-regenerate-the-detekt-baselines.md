# E01-36 — Regenerate the detekt baselines

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `DetektConventionPlugin` applies the same two excludes to `DetektCreateBaselineTask` that it
      already applies to `Detekt`, sharing one declaration rather than copying it
- [ ] every gate-bearing baseline is then rewritten by its own `detektBaseline*` task, run from a
      green tree, and the commit carries exactly what those tasks wrote — never a hand edit
- [ ] `baseline-data-main.xml` lands near 48 entries, down from 577, and no baseline names a
      generated file: no `*Queries.kt`, `EmmDatabaseData*.kt`, `IosSupabaseConfig.kt`, `BuildInfo.kt`
- [ ] no baseline file is added for a source set whose check task reports zero issues
- [ ] the stem files `baseline-{androidApp,data,domain}.xml` are untouched — they belong to the
      plain `detekt` task, which is not a gate
- [ ] the `androidApp` devDebug/prodDebug entry gap is gone, or the closing commit says why the two
      flavors legitimately differ
- [ ] the closing commit states the entry count per file, before and after
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Measured 2026-08-24: 529 of the 577 entries in `baseline-data-main.xml` name SQLDelight-generated
code detekt stopped analysing at `c94e2901`. A first attempt to sweep them by regeneration made it
worse — 904 entries — because in detekt `2.0.0-alpha.6` `Detekt` and `DetektCreateBaselineTask` are
both `SourceTask` subclasses and **neither extends the other**, so `tasks.withType<Detekt>()` never
reaches baseline generation. Check tasks filter generated roots; baseline tasks do not.

Fixing the plugin changes what baseline tasks *see*, never what the gate *enforces*, and a regen on
a green tree grandfathers nothing new by construction.
