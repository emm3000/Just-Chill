# E14-04 — Strip the profile screen to what is true

**Epic:** [E14 — Information hierarchy](../epics/E14-information-hierarchy.md)

## Done when

- [ ] `ProfileScreen` no longer shows a "Cuentas" row (the tab is its door).
- [ ] The "Iniciar sesión · Respalda tus datos en la nube" row renders only while
      `SNAPSHOT_BACKUP_ENABLED` is `true`, inside the Respaldo section — the same gate that already
      hides `BACKUP_LOCAL_ONLY_WARNING` when the flag flips, so the two copies can never coexist.
- [ ] Row subtitles carry data, not labels: categories split by type, recurring count with the
      monthly outflow, the last export date or "Nunca".
- [ ] The backup note renders as a hairline card directly under "Importar respaldo".
- [ ] `./gradlew qualityGate --rerun-tasks` and `assembleDevDebug` green; an emulator screenshot
      matches the canvas' "Perfil · propuesta" artboard.

## Context

Read `docs/work/epics/E01-snapshot-backup.md` before touching the Respaldo rows. The login row is
the only door to `AuthRoute` (`ProfileEntries.onSignInClick`), which serves E01's cloud backup —
gate it, never delete it. `BackupSection` already reads the flag twice; reuse, do not add a reader.
