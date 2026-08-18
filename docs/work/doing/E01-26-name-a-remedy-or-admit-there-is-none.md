# E01-26 — The backup row offers a remedy for a failure the user cannot act on

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `LocalDatabase` either moves to `toFailureAction`'s no-remedy group, or its action names
  something the user can actually do
- [ ] a test pins which group each reason belongs to, so the split stops being a judgement call

## Context

`BackupRowText.toFailureAction` puts `LocalDatabase` in the has-a-remedy group, but its string is a
statement — "no pude leer tus datos" — not an action. Pre-existing; E01-12 checked the grouping and
deliberately did not widen into it.
