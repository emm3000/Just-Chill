# E06-08 — Stop painting spend red

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] no amount is tinted because it is a spend or because it is negative — `DESIGN_SYSTEM.md` §4
      holds on every screen, from the transaction row to the report hero
- [ ] `AmountTone.Neg` either renders the same as `Neutral` or stops existing; a tone whose whole job
      is tinting a negative cannot survive the rule that forbids it
- [ ] `success` still tints income — §4 removes only the negative half
- [ ] every destructive or broken use of `danger` is untouched: the "Borrar" actions, the delete
      dialogs' chrome, `BackupRowSeverity.Danger`, and the note field's over-limit counter
- [ ] a deliberate call is made and recorded for the two ambiguous cases — the catch-up marker on a
      pending row, and a negative savings rate — since "needs your attention" is arguably broken
      rather than money leaving
- [ ] `@Preview` coverage shows an income and a spend side by side on at least one screen
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`AccountsScreen`'s `TotalBalanceRow` obeys §4 and every other amount surface does not, which is how
the contradiction surfaced — the new screen looked wrong against the old ones.

A sweep found roughly eleven amount-tinting sites across the recurring, transaction, seetransactions
and report features. Fixing `AmountTone` at the atom reaches most of them at once; the raw
`colors.danger` amount tints need finding individually.
