# E11-08 — Sweep the docs

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-07

## Done when

- [ ] The root `CLAUDE.md` describes an Android-only Kotlin project — no `iosApp`, SKIE,
      `JustChillKit`, `commonMain` or `androidHostTest` claim survives — and it is still ≤ 150
      lines.
- [ ] Every module `CLAUDE.md` names its new plugin and its new source layout.
- [ ] `PROGRESS.md`, `PERSISTENCE.md`, `CODE_QUALITY.md`, `WORKFLOW.md` and `RELEASE_CHECKLIST.md`
      name no iOS gate step and no KMP source set.
- [ ] `git grep -lni 'iosMain\|SKIE\|JustChillKit\|commonMain\|androidHostTest\|\bios\|SwiftUI\|Xcode'
      -- docs/ '*CLAUDE.md' ':!docs/archive' ':!docs/adr' ':!docs/work/epics/E11-android-only.md'`
      returns nothing. (Plain `ios` without the leading `\b` also matches Spanish words like
      `servicios`/`criterios` and can never go to zero — checked empirically before writing this.)
- [ ] The headers of ADR 003 and ADR 005 each declare they are superseded by ADR 011.
- [ ] `./gradlew qualityGate --rerun-tasks` passes.
