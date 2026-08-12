# :androidApp — CLAUDE.md

Thin Android entry point. Almost nothing belongs here. Since the S1 extraction the split is two
layers, not one: **the ViewModels, the MVI core and the whole Koin graph live in `:presentation`**,
and only the Compose UI lives in `:ui-android`. Read `presentation/CLAUDE.md` before adding a
feature, `ui-android/CLAUDE.md` before adding a screen.

Root package: `com.emm.justchill`. `minSdk = 28`, `compileSdk = 37`.
Depends on `:ui-android`, `:domain` and `:data` — the last two **directly**, not transitively,
because `AndroidPlatformModule` constructs the SQLDelight driver itself.

## What actually lives here

```
MainActivity.kt                        edgeToEdge + setContent { AppNavHost() }
EmmApp.kt                              Application: startKoin + bootstrapAppGraph
core/AndroidPlatformModule.kt          the injected platformModule
core/DefaultDispatcher.kt              Android impl of :presentation's DispatchersProvider
                                       interface — Koin-bound, NOT an expect/actual
core/CrashReportingSyncLogger.kt       SyncLogger sink → Crashlytics (iOS binds a println one)
core/platform/CurrentActivityHolder.kt
hh/auth/ActivityGoogleSignInLauncher.kt + GoogleCredentialClient.kt
components/EmmAmountChill.kt + EmmComponentsPreview.kt   (Android-only @Preview surface)
```

If you find empty `core/preferences/`, `core/sync/`, `core/ui/` or `hh/di/` directories here, they
are local residue from the KMP migration (git does not track empty directories). Delete them;
nothing belongs in them anymore.

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits:

- DB `single` (driver + onCreate seed) + `EmmDatabaseData`
- `Settings` backend (`SharedPreferencesSettings`, file name `justchill_prefs` — it used to be named
  after `Build.ID`, which wiped prefs on every OS update; fixed in `4d2e204` with a one-time migration)
- `SupabaseConfig`
- `GoogleSignInLauncher` (+ `GoogleCredentialClient`)
- `appVersion` / `googleServerClientId` — named `String`s from `BuildConfig`
- `DispatchersProvider`, `CurrentActivityHolder`
- `SyncLogger` → `CrashReportingSyncLogger` (Crashlytics is Android-only; iOS binds
  `PrintlnSyncLogger` in `KoinIos.kt`)

`EmmApp` calls `startKoin { modules(appModules(androidPlatformModule) + experiencesModule) }` then
`bootstrapAppGraph(koin)`. `startKoin` can't be shared — it needs `androidContext()` /
`androidLogger()` from koin-android. A new **feature** module is registered in `appModules()` in
`presentation/src/commonMain/kotlin/com/emm/justchill/core/AppGraph.kt`, **not** here — and its
ViewModel goes into `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`, which is the only mechanical guard
against a missing binding. It is not in `:ui-android`: that module has no `commonMain` at all.

## Product flavors

Dimension `tier`:
- `dev` — `applicationIdSuffix = ".dev"`. Also carries `src/dev/kotlin/.../experiences/` — a
  personal Compose playground (calendar, padding, timer picker, JSON-from-assets) that is **not part
  of the product**. It ships its own `experiencesModule`; `prod` has a no-op stub of the same file.
- `prod` — release signing via `keystore.properties`, Firebase Crashlytics.

Crashlytics is declared for all variants, but `src/dev/AndroidManifest.xml` sets
`firebase_crashlytics_collection_enabled=false`, which keeps the privacy policy's "build the dev
flavor for a telemetry-free app" claim true. Firebase **Analytics is not used**, and contrary to what
this file used to say it is not declared either: the catalog holds only `firebase-bom` and
`firebase-crashlytics`, and both are consumed in `build.gradle.kts`. There is no orphan to remove.

`versionCode` is the git commit count, `versionName` the latest **release** tag (`git describe
--match "v[0-9]*"` — the filter is load-bearing, the repo is full of non-release tags like
`pre-kmp`) — both computed at configure time in `build.gradle.kts`.

## Testing

- `./gradlew :androidApp:testDevDebugUnitTest`
- `MainDispatcherRule` at `androidApp/src/test/kotlin/com/emm/justchill/MainDispatcherRule.kt` —
  **use it in every ViewModel test that touches `viewModelScope`**.
- **The MockK ViewModel tests live here, not in `:presentation`**, even though the ViewModels
  themselves are in `presentation/src/commonMain`: they sit in the same package
  (`hh/home/HomeViewModelTest.kt`, etc.) and rely on MockK's JVM engine. That placement is
  deliberate — keep it unless you move the whole suite.
- No instrumented tests: `androidApp/src/androidTest/` does not exist.

## Anything UI

Screens, navigation and theme are in `:ui-android`; ViewModels, MVI base classes and feature DI are
one layer further down, in `:presentation`. If you find yourself adding a composable here, it is
either a `@Preview` host or it is in the wrong module.
