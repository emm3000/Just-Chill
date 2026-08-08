# :androidApp — CLAUDE.md

Thin Android entry point. Since the KMP migration, **the UI, ViewModels and Koin wiring live in
`:shared-ui`** — read `shared-ui/CLAUDE.md` before adding a feature. Almost nothing belongs here.

Root package: `com.emm.justchill`. `minSdk = 28`, `compileSdk = 37`.
Depends on `:shared-ui` (and transitively `:domain`, `:data`).

## What actually lives here

```
MainActivity.kt                        edgeToEdge + setContent { AppNavHost() }
EmmApp.kt                              Application: startKoin + bootstrapAppGraph
core/AndroidPlatformModule.kt          the injected platformModule
core/DefaultDispatcher.kt              DispatchersProvider actual
core/platform/CurrentActivityHolder.kt
hh/auth/ActivityGoogleSignInLauncher.kt + GoogleCredentialClient.kt
components/EmmAmountChill.kt + EmmComponentsPreview.kt   (Android-only @Preview surface)
```

`core/preferences/`, `core/sync/`, `core/ui/`, `hh/di/` are **empty leftover directories** from the
migration. Delete them when you next touch this module; don't put anything back in them.

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits:

- DB `single` (driver + onCreate seed) + `EmmDatabaseData`
- `Settings` backend (`SharedPreferencesSettings`, file name `justchill_prefs` — it used to be named
  after `Build.ID`, which wiped prefs on every OS update; fixed in `4d2e204` with a one-time migration)
- `SupabaseConfig`
- `GoogleSignInLauncher` (+ `GoogleCredentialClient`)
- `appVersion` / `googleServerClientId` — named `String`s from `BuildConfig`
- `DispatchersProvider`, `CurrentActivityHolder`

`EmmApp` calls `startKoin { modules(appModules(androidPlatformModule) + experiencesModule) }` then
`bootstrapAppGraph(koin)`. `startKoin` can't be shared — it needs `androidContext()` /
`androidLogger()` from koin-android. A new **feature** module is registered in `appModules()` in
`shared-ui/commonMain/core/AppGraph.kt`, **not** here.

## Product flavors

Dimension `tier`:
- `dev` — `applicationIdSuffix = ".dev"`. Also carries `src/dev/kotlin/.../experiences/` — a
  personal Compose playground (calendar, padding, timer picker, JSON-from-assets) that is **not part
  of the product**. It ships its own `experiencesModule`; `prod` has a no-op stub of the same file.
- `prod` — release signing via `keystore.properties`, Firebase Analytics + Crashlytics.

`versionCode` is the git commit count, `versionName` the latest git tag — both computed at
configure time in `build.gradle.kts`.

## Testing

- `./gradlew :androidApp:testDevDebugUnitTest`
- `MainDispatcherRule` at `androidApp/src/test/kotlin/com/emm/justchill/MainDispatcherRule.kt` —
  **use it in every ViewModel test that touches `viewModelScope`**.
- **The MockK ViewModel tests live here, not in `shared-ui`**, even though the ViewModels themselves
  are in commonMain: they sit in the same package (`hh/home/HomeViewModelTest.kt`, etc.) and rely on
  MockK's JVM engine. That placement is deliberate — keep it unless you move the whole suite.
- No instrumented tests: `androidApp/src/androidTest/` does not exist.

## Anything UI

Screens, ViewModels, navigation, theme, MVI base classes, feature DI — all in `:shared-ui`.
If you find yourself adding a composable here, it is either a `@Preview` host or it is in the
wrong module.
