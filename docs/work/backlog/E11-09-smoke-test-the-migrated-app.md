# E11-09 — Smoke-test the migrated app on a device

**Epic:** [E11 — Android only](../epics/E11-android-only.md)
**Blocked by:** E11-08

## Done when

- [ ] `assembleDevDebug` installs on an emulator and the app opens without crashing.
- [ ] Every bundled font renders (Inter, IBM Plex Mono) and the Google sign-in vector draws —
      `:ui-android` changed how AGP processes its resources, and that failure is a runtime
      `MissingResourceException`, not a build error.
- [ ] A transaction, a category, an account and a recurring template each save and reappear after a
      cold start, against a database restored from a pre-E11 build.
- [ ] The back stack survives process death on at least one pushed route — `RouteSerializationTest`
      compiles but only a device proves `rememberNavBackStack` still resolves the serializers.
- [ ] A snapshot backup writes and restores.

## Context

608 files changed path and SQLDelight's source directory moved. This class of change compiles green
and dies at runtime; the gate cannot see any of the boxes above.
