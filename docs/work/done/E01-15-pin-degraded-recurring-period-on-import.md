# E01-15 — Pin the degraded recurring period on import

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] a test pins that a template whose `lastConfirmedPeriod` is malformed, or whose year falls
      outside `MIN_PERIOD_KEY_YEAR..MAX_PERIOD_KEY_YEAR`, still imports — the template intact, the
      mark null
- [x] the epic carries the constraint that an unparseable settled mark degrades the field and never
      the row, so a later consistency argument cannot turn it into a dropped template

## Context

`parsedType`/`parsedFrequency` reject the whole row because an unreadable type or frequency leaves
it unusable; an unreadable mark leaves the template fully usable. Re-minting is bounded by
`MAX_CATCH_UP_MONTHS` and every period is user-confirmed through `HomeIntent.ConfirmRecurring`,
so no transaction is written without a prompt.
