# E01-17 — Read the iOS app version from the bundle

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `KoinIos.kt`'s `appVersion` binding reads the real bundle version instead of the literal
      `"1.0.0"`
- [ ] an iOS snapshot's stamped `appVersion` (payload and manifest) matches the build that produced
      it

## Context

Android reads `BuildConfig.VERSION_NAME` for the same binding; every iOS snapshot today stamps a
false version.
