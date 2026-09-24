---
description: Run the quality gate and the dev build, summarize failures
allowed-tools: Bash(./gradlew:*) Read
disable-model-invocation: true
---

Run the standard pre-commit checks for this Android repo:

1. `./gradlew qualityGate` — the gate, whose task list is the `qualityGate` task `description` in `build-logic/.../QualityGateConventionPlugin.kt`. Report any violation or failing test.
2. `./gradlew assembleDevDebug` — the gate excludes the build on purpose; report any compile error.

The gate is that plugin plus the unit tests the library plugins add and `:androidApp`'s own `tasks.named("qualityGate")` block. The gate task is the whole check; no lighter command substitutes for it.

Locally the gate's `detekt` rewrites formatting in place instead of failing on it (ADR 018); list the files it touched.

Group findings by module (`:core:*`, `:feature:*`, `:androidApp`, `:build-logic`). For each violation include `file:line` and the check or test name. Test tasks go `UP-TO-DATE` across sessions; add `--rerun` per task when a real run is needed.

Do NOT fix anything in this turn — only report. End with one line: **"ready to commit"** if both pass, or a short list of what to fix next.
