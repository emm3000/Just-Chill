# E07-07 — Restore the missing test-compilation detekt baselines

**Epic:** [E07 — baseline burndown](../epics/E07-baseline-burndown.md)

## Done when

- [ ] `:data:detektDebugUnitTest`, `:presentation:detektDebugUnitTest` and
      `:ui-android:detektDebugUnitTest` each resolve a baseline file that exists on disk (confirm
      with `--dry-run` plus a printed `baseline` property, or by regenerating via
      `detektBaselineTest` and inspecting what it writes)
- [ ] either `config/detekt/baseline-{data,presentation,ui-android}-debugUnitTest.xml` exist with
      real content, or the closing commit records that the source sets are clean and no baseline is
      needed
- [ ] `./gradlew qualityGate --rerun-tasks` passes

## Context

`DetektConventionPlugin.kt` sets one extension-level `baseline.set(file(".../baseline-$name.xml"))`.
The plugin's own resolution (`existingVariantOrBaseFile`, `detekt-gradle-plugin-2.0.0-alpha.6`
sources) tries `baseline-<module>-<variant>.xml` first, then falls back to the bare
`baseline-<module>.xml`, then to no baseline at all. `:data`, `:presentation` and `:ui-android` have
`-debug.xml`/`-release.xml` (main variants) but no bare `baseline-{data,presentation,ui-android}.xml`
stem and no `-debugUnitTest.xml` — so `detektDebugUnitTest`, one of the two tasks `detektTest`
(and therefore `qualityGate`) depends on, currently runs with **no baseline at all** on all three
modules' `src/test`. Not a crash — Gradle treats the absent property as "no baseline configured" —
but it means a future `src/test` violation has no legitimate way to be baselined until this file
exists. Found auditing `.github/**` for E04-07; out of scope there (needs a `build-logic` change,
and that session ran no Gradle task).
