# E06-06 — Refresh `today` across midnight

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] a pending recurring movement that comes due while the app sits open appears without the user
      changing month, backgrounding the app, or killing it
- [ ] the browsed month follows the calendar across a month boundary — an app left open on the 31st
      shows the new month on the 1st, rather than a month the user never chose
- [ ] the refresh is driven by an injected `Clock` and `TimeZone`, never a device default, and a test
      pins the rollover by advancing that clock rather than by waiting
- [ ] the fix does not poll on a timer tighter than the problem warrants — a day boundary is the
      event, not a tick
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`SeeTransactionsViewModel.today()` is re-derived whenever its pending subscription restarts, which a
month change does — but nothing restarts it on its own, so an app left open across midnight keeps
serving yesterday's date to `GetPendingRecurringMovementsUseCase`.

This predates E06: Home had the identical gap, and it survived the move to Ver rather than being
introduced by it. It was found while reviewing E06-03 and deliberately left out of that ticket, which
was a behaviour-preserving migration.
