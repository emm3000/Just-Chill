# E06-10 — Put Report on `TodayFlow`

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] `ReportViewModel` derives the current date through `TodayFlow`, not through a `Clock` and a
      `TimeZone` of its own — a report left open across midnight stops calling August the current
      month on 1 September
- [ ] `isCurrent` and the trends window read that one date, so no two of them can disagree
- [ ] `Clock`/`TimeZone` leave the constructor unless a caller still needs them for something other
      than "what day is it"; if any survive, the reason is named where the next writer reads it
- [ ] `a month rollover corrects isCurrentMonth with no month move` still passes, or its replacement
      pins the same guarantee — Report's browsed month must NOT follow the calendar, unlike Ver's
- [ ] a test drives the rollover by moving an injected fake, never by waiting
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

E06-06 built `TodayFlow` (`presentation/.../core/time/`) and wired only `SeeTransactionsViewModel`.
`ReportViewModel` re-reads `YearMonth.current(clock, zone)` per tap. Ver tracks the calendar, Report
never moves its month: reuse the date, not the policy. Tests take a fake (`presentation/CLAUDE.md`).
