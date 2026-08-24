# E06-05 — Rename the Spanish identifiers in Report

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] `ReportTab`'s two entries read as English identifiers, and every `when` branch, default and
      test that names them follows
- [ ] `MesContent`, `ReportScreenMesPreview` and `ReportScreenMesGastosPreview` in `ReportScreen`
      carry English names
- [ ] `ReportShareFormatter.buildMesShareText` carries an English name, matching its already-English
      sibling `buildTrendsShareText`
- [ ] every user-facing string is byte-identical afterwards — `"Mes"`, `"Tendencias"` and the share
      text stay Spanish, addressed as `tú`
- [ ] `rg` over `:presentation` and `:ui-android` finds no declaration whose name is a Spanish word
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

The root `CLAUDE.md` requires English for every identifier and Spanish only in user-data values.
These five declarations predate the rule's enforcement; the report feature is the last place they
survive.

`ReportTab` is not `@Serializable` and reaches no store — it is in-memory UI state, so renaming its
entries needs no migration and cannot break a restore. That is what makes this a safe mechanical
rename rather than a schema change.
