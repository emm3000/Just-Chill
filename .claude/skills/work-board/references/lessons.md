# Where the rules come from

Each rule in `SKILL.md` exists because something went wrong once. Kept here so the rules stay short.

## The audit-before-working rule

**E01-15, the direction was harmful.** The ticket said: reject the whole recurring row when
`lastConfirmedPeriod` fails `parsePeriodKey`, for consistency with `parsedType`/`parsedFrequency`.
Implementing it would have deleted the user's template on restore — trading a declinable prompt for
silent data loss in the one path where data loss is unforgivable.

The consistency argument used the wrong axis. The question is whether the row is still usable: an
unreadable `type` or `frequency` leaves it unusable, an unreadable settled mark does not. The fact
that settled it was found by grep, not by reasoning — pending periods reach the user through
`HomeIntent.ConfirmRecurring`, so nothing auto-posts.

No code review would have caught this. There was no code yet.

**E01-17, a condition nothing could check.** `Done when: an iOS snapshot's stamped appVersion
matches the build that produced it` — `SNAPSHOT_BACKUP_ENABLED` has never been true, no iOS
snapshot exists, and there is no iOS test harness. The ticket also left a decision unmade: what the
binding does when the bundle key is absent. Writing a falsifiable condition forces you to ask who
will check it, and that question is what exposes the missing decision.

## The premise rule

E01-15's premise was "No coverage." Four tests already existed in `DefaultBackupRepositoryImportTest`,
one named `...restores with no mark rather than being dropped` — the decision had already been made
and pinned. The premise came from a session where the code was verified (`RecurringMovementDto.kt`
does null the field) and the tests were never grepped. Half the claim was checked; both halves were
reported as checked.

## The constraint rule

A false constraint outlives everything. A plan asserted `BackupOrchestrator` holds the shared
`SyncMutex`; the file contains no lock at all, so an upload and an account deletion were never
serialized against each other and the doc said the opposite. It survived months because prose is
never executed.

Both closed tickets earned a constraint line, and both times it was added late — E01-17 needed an
`--amend`. Two out of two makes it a step, not an oversight.

## The proof rule

A `qualityGate` reporting `12 executed, 229 up-to-date` right after targeted `--rerun` runs
re-verified nothing: the suite that had failed was already up-to-date and never ran again. Force it
with `--rerun-tasks`, which also loads the machine and reproduces the conditions a flake needs —
that is how `BackupOrchestratorHealthTest`'s wall-clock timeout was confirmed as a flake and not a
logic failure.
