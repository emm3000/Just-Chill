# E11-09 — Smoke-test the migrated app on a device

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-08

## Done when

- [ ] `assembleDevDebug` installs over the emulator's existing `v2.6.0` install — a real pre-E11
      build (`ff036fd3`, ancestor of `origin/trunk`) — and the app opens without crashing.
      `dumpsys package com.emm.justchill.dev` reports the new `versionName`, naming which build ran.
- [ ] That database survives the upgrade: `transactions` 5, `categories` 23, `accounts` 1,
      `recurring_movements` 2, `user_version` 6. A fresh empty database is the failure this box
      exists to catch. The emulator has no `sqlite3` — read the file out with
      `adb shell run-as com.emm.justchill.dev cat databases/com.emm.data.db`.
- [ ] Every bundled font renders (Inter, IBM Plex Mono) and `ic_google.xml` draws — `:ui-android`
      changed how AGP processes its resources, and that failure is a runtime `MissingResourceException`,
      not a build error. One screenshot per surface is the proof.
- [ ] A transaction, a category, an account and a recurring template each save and reappear after a
      cold start.
- [ ] The back stack survives process death on a pushed route: background the app, `adb shell am kill
      com.emm.justchill.dev`, relaunch. `RouteSerializationTest` compiles, but only a device proves
      `rememberNavBackStack` still resolves the serializers.
- [ ] The manual export/import drill in `docs/RELEASE_CHECKLIST.md` passes — NOT the snapshot path:
      `SNAPSHOT_BACKUP_ENABLED` (`n` in `BackupKillSwitch.kt`) is `false`, so `BackupOrchestrator`
      never starts and there is no snapshot to write.

## Context

608 files changed path and SQLDelight's source directory moved. This class of change compiles green
and dies at runtime; the gate cannot see any of the boxes above.
