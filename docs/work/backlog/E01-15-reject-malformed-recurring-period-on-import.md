# E01-15 — Reject a malformed recurring period on import

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `RecurringMovementDto.toEntityOrNull()` rejects the row (returns `null`) when
      `lastConfirmedPeriod` is non-null but fails `parsePeriodKey`, instead of silently dropping
      only that field
- [ ] a test pins that a malformed or out-of-range `lastConfirmedPeriod` no longer imports as an
      unsettled template

## Context

Today `lastConfirmedPeriod?.let(::parsePeriodKey)?.let(::periodKey)` nulls the field alone on a
parse failure, re-minting months the user already closed; `parsedType`/`parsedFrequency` already
reject the whole row on the same kind of failure.
