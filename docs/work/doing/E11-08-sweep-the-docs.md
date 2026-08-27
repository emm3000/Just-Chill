# E11-08 — Sweep the docs

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-07

## Done when

- [ ] The root `CLAUDE.md` describes an Android-only Kotlin project — no `iosApp`, SKIE,
      `JustChillKit`, `commonMain` or `androidHostTest` claim survives — and it is still ≤ 150 lines.
- [ ] Every module `CLAUDE.md` names its new plugin and its new source layout. `:data` and `:domain`
      were updated at conversion time; `:presentation`, `:ui-android` and `:androidApp` were not.
- [ ] `PROGRESS.md`, `PERSISTENCE.md`, `CODE_QUALITY.md`, `WORKFLOW.md` and `RELEASE_CHECKLIST.md`
      name no iOS gate step and no KMP source set.
- [ ] Six work-board files carry a stale path or a dead iOS rationale — tickets `E01-32`, `E01-38`,
      `E07-06` and epics `E01`, `E02`, `E06`. `E01-32` cites a task that no longer exists, so it
      cannot be run as written. Judge each on its own: correct a stale path, delete a constraint
      whose only reason was iOS, restate one that survives iOS without it.
- [ ] This epic's own constraints hold. Its `contributeToQualityGate` line names a function E11-07
      deleted, and its conversion-order line is spent.
- [ ] `git grep -lni 'iosMain\|SKIE\|JustChillKit\|commonMain\|androidHostTest\|\bios\|SwiftUI\|Xcode'
      -- docs/ '*CLAUDE.md' ':!docs/archive' ':!docs/adr' ':!docs/work/epics/E11-android-only.md'`
      returns nothing. (Plain `ios` without the leading `\b` also matches Spanish words like
      `servicios`/`criterios` and can never go to zero — checked empirically before writing this.)
- [ ] The headers of ADR 003 and ADR 005 each declare they are superseded by ADR 011. Neither does
      today: ADR 003 names only ADR 007, ADR 005 names nothing.
- [ ] The diff touches only `*.md`, so no gate task can regress — `git diff --stat` proves it, and
      that is the whole check this unit needs.
