# E11 — Android only (closed)

**Decision:** [ADR 011](../adr/011-android-only-drop-the-kmp-build-and-the-ios-target.md)

## Why

iOS is dead: ADR 011 deletes the SwiftUI app, the Kotlin/Native targets and SKIE. What replaces the
multiplatform build is a plain Android Kotlin one — three `com.android.library` modules over a
`kotlin("jvm")` `:domain`, one source set each.

## Where the constraints went (E11-10)

Every E11 ticket is closed, so this file holds no live rule. Its six invariants were resolved one at
a time; nothing below is the copy to read.

Rehomed, because nothing else stated them:

- The `kotlin-jvm` alias being unversioned → root `CLAUDE.md` `## Gotchas`. The alias and
  build-logic's pin already carried the mechanism in comments; what was missing was the symptom —
  opt-in errors in `:domain` source that name nothing about the classpath.
- `multiplatform-settings` staying → its own `gradle/libs.versions.toml` entry, which was itself
  still describing `commonMain` and `NSUserDefaultsSettings (iOS)`.
- `.sq`/`.sqm` moving but never renaming → `docs/PERSISTENCE.md` `## Schema`, beside the sentence
  that says where those files live. E02 owns test shape and delegates mechanics there.

Deleted as duplicates, because a rule in two places diverges:

- The per-variant detekt baseline split — `docs/CODE_QUALITY.md` `### One baseline file per analysis
  task` says it in far more detail.
- `:presentation` carrying no Compose dependency — `presentation/CLAUDE.md` `## The one rule` owns it
  with the ADR 011 Decision 4 demotion and the `rg` check, and `docs/WORKFLOW.md` puts it on the
  mandatory-review list.
- `:domain` being `kotlin("jvm")` on purpose — `domain/CLAUDE.md` opens with it and states the reason
  better: with no Android artifact on the compile classpath, `android.*` cannot resolve at all, which
  is a dependency-graph guarantee rather than a convention. Its second half, per-module test-task
  registration, is in root `CLAUDE.md`'s gate gotcha.
