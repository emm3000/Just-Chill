---
description: Run the quality gate and the dev build, summarize failures
allowed-tools: Bash(./gradlew:*) Read
disable-model-invocation: true
---

Run the standard pre-commit checks for this Android repo:

1. `./gradlew qualityGate` — detekt per module, every host test suite, `:data`'s instrumented compile, `verifySqlDelightMigration`, `:build-logic:test`. Report any violation or failing test.
2. `./gradlew assembleDevDebug` — the gate excludes the build on purpose; report any compile error.

`qualityGate` is defined once in `build-logic/.../QualityGateConventionPlugin.kt` plus each module's own `tasks.named("qualityGate")` block. Never substitute plain `./gradlew detekt`: it covers strictly less.

Group findings by module (`:domain`, `:data`, `:presentation`, `:ui-android`, `:androidApp`, `:build-logic`). For each violation include `file:line` and the rule/test name. Test tasks go `UP-TO-DATE` across sessions; add `--rerun` per task when a real run is needed.

Do NOT fix anything in this turn — only report. End with one line: **"ready to commit"** if both pass, or a short list of what to fix next.
