---
status: accepted
date: 2026-09-18
amends: ADR 011 (the `qualityGate` legs it lists), ADR 015 (the convention plugin list)
---
# No linter: the gate compiles and tests

detekt cost more than it caught. Two tickets in a row (#157, #173) were about
keeping it honest rather than about the product, and its analysis classpath
breaks silently on every AGP move: type-resolution rules go quiet wherever a
symbol does not resolve and the task still passes, so a green gate was never
evidence that the rules ran. The findings it did report were style findings on
a single-author app, arbitrated by review anyway.

## Decision

detekt is removed and no linter replaces it. The convention plugin, its
application in the application, library and JVM library plugins, the version
catalog entries, `config/detekt/` with its thirteen baselines, the gate wiring
(`qualityGate { detektTasks }` and the `detektMain` / `detektTest` allowlist)
and the CI report upload are gone; the extension the wiring existed for is gone
with them, so `QualityGateExtension` and `BuildConventions.baselineNameOf` are
deleted rather than left empty.

`./gradlew qualityGate` keeps every other check: `checkModuleBoundaries`,
`checkComposeFreeViewModels`, `checkSqlDelightSnapshots`,
`compileDebugAndroidTestKotlin`, `compileReleaseKotlin`,
`verifySqlDelightMigration`, `:build-logic:convention:test` and each module's
unit tests. The `qualityGate` task `description` stays the single source of
that list.

The complexity limits detekt enforced survive as review conventions in
`.claude/rules/kotlin-style.md` — nesting, return count, functions per file,
function length. They are now the reviewer's, not a task's.

## Consequences

Nothing mechanical rejects a formatting or complexity regression; a PR review
is the only net, which is what it already was for the rules detekt could not
see. `:androidApp`'s `prodRelease` variant used to be compiled only as a side
effect of `detektProdRelease`, whose analysis classpath was that compile task's
output, so its build file now names `compileProdReleaseKotlin` on the gate
directly. `gradle.properties` keeps a 6144m Gradle heap sized when detekt
analysed inside that daemon; lowering it is its own measurement.

ADR 011's `qualityGate` legs and ADR 015's convention plugin list are amended:
read them without their detekt entries. The rest of `docs/adr/` stays as
written, and git holds the config and the baselines.
