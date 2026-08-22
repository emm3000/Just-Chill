# E05-11 — Rehearse the v6 restore

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocks:** shipping any build carrying schema v6

## Done when

- [ ] a backup is exported from the installed pre-bump build (`0842d7d` — DB v5, payload v3) and
      kept, with Home's `Saldo total` plus the Cuentas and Categorías counts recorded first
- [ ] that same v3 file imports on the v6 build reporting the same movimientos and recurrentes, no
      préstamos/abonos clause, and the three recorded figures unchanged
- [ ] a v4 file exported from the v6 build, holding at least one loan and one abono, restores onto a
      clean install with every figure above intact and the loan ledger present

## Context

The author runs `0842d7d` daily on real data, so the pre-bump build needs no emulator — it is the
device, and the production snapshot no fixture substitutes for comes off it.

`5.sqm` is additive — two `CREATE TABLE`, four `CREATE INDEX`, nothing existing read, rebuilt or
dropped — so the migration cannot lose a row. The unknown is **format, not schema**: only the real
file exercises `ExportPayloadV3Dto.toCurrent()` against real data.

The import snackbar is a partial witness: `buildImportDoneMessage` never reports accounts or
categories, and omits any clause whose count is zero. Read those from the app, not the toast.
