# E01-17 — Read the iOS app version from the bundle

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] `KoinIos.kt` binds `appVersion` from the main bundle's `CFBundleShortVersionString`, with no
      literal version string left in the file
- [x] the binding falls back to `"unknown"` when the key is missing, never to a plausible-looking
      version
- [x] `./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64` passes

## Context

Android binds the same qualifier from `BuildConfig.VERSION_NAME`, and the value is stamped into
every exported payload and its manifest, so a false version misattributes a backup to a build that
never produced it. There is no iOS test harness, so the compile and link are the only mechanical
checks available.
