---
status: accepted
date: 2026-09-24
---
# detekt comes back as a flat lint

ADR 016 removed detekt because its per-variant type-resolution tasks went quiet
on every AGP move and needed thirteen baselines. What it enforced still matters,
so it returns in the one shape that has neither problem: one plain task per
module, one config, no classpath and no baseline.

## Decision

detekt 2.0.0-alpha.6, the only line built against Kotlin 2.4.10 and AGP 9.3.3,
runs with the ktlint wrapper and compose-rules 0.6.6. `justchill.detekt`,
applied by `justchill.android.application`, `justchill.android.library` and
`justchill.jvm.library`, registers a single `detekt` task of detekt's own
`Detekt` type. It does not apply the `dev.detekt` Gradle plugin: that plugin,
and its base plugin, register `detektMain`, `detektTest` and a task per variant
and per source set on every Kotlin module, whatever the extension says.

- The task reads every `.kt` under the module's `src/`, so `test`, `androidTest`
  and any future source set are linted without wiring.
- `config/detekt/detekt.yml` is built upon detekt's default config and is the
  only file that turns a rule off or changes a limit. `allRules` stays off. No
  baseline exists: a finding is fixed in code or its rule is changed there.
- `detekt` is on `qualityGate`. `buildDev` uploads each module's
  `build/reports/detekt/detekt.sarif`.
- Formatting corrects itself locally and fails CI. `autoCorrect` is on unless
  the `CI` environment variable is `true`, which GitHub Actions sets on every
  runner. detekt 2.0 reports an auto-corrected ktlint finding as suppressed, and
  a suppressed finding never fails the task, so a CI run with `autoCorrect` on
  would rewrite its own checkout and pass. Reading `CI` keeps every workflow
  command as it was and holds `uploadApk` and `uploadRelease` to the same rule.

## Why the flat task cannot go quiet

With no classpath, detekt runs in `light` analysis mode: it parses the sources
itself and never reads compiler output. The rules that need type resolution are
switched off by detekt before analysis starts, the same set on every run, instead
of each one degrading file by file when a symbol fails to resolve. The task's
inputs are the source tree and one YAML file, and an AGP or Kotlin move changes
neither. `ConventionPluginTest` pins the sources, the config, the empty
classpath, the absent baseline, the single task name and the gate's closure.

## Consequences

The type-resolution rules are gone for good; the compiler and review keep what
they used to catch. The complexity limits in `.claude/rules/kotlin-style.md` are
the config's now, and that file points to it. Running `./gradlew qualityGate`
locally can rewrite formatting in place, and the rewrite is part of the commit.
detekt analyses inside the Gradle daemon again, so the 6144m heap in
`gradle.properties` is back to hosting it; lowering it is still its own
measurement.

This supersedes ADR 016. Its other removals stand: `QualityGateExtension`,
the thirteen baselines and the per-variant allowlist stay deleted.
