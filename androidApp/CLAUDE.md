# :androidApp — CLAUDE.md

Thin Android entry point. Almost nothing belongs here: the ViewModels, the MVI core and the whole
Koin graph live in `:presentation`, and the Compose UI lives in `:ui-android`. Read
`presentation/CLAUDE.md` before adding a feature, `ui-android/CLAUDE.md` before adding a screen.

`com.android.application` on AGP 9's built-in Kotlin support — no separate `kotlin.android` plugin,
same as the three libraries. Root package `com.emm.justchill`, `minSdk = 28`, `compileSdk = 37`.
Depends on `:ui-android`,
`:domain` and `:data` — the last two **directly**, not transitively, because
`AndroidPlatformModule` constructs the SQLDelight driver itself.

`src/main` holds `MainActivity`, `EmmApp`, and the Android halves of ports `:presentation` declares:
the platform Koin module, `DispatchersProvider`, the Crashlytics `DiagnosticsLogger` sink,
`CurrentActivityHolder`, the Google sign-in launcher, plus the `@Preview` host in `components/`.
If you are adding anything else here, it belongs in another module.

## Platform Koin module

`androidPlatformModule` (`core/AndroidPlatformModule.kt`) supplies only genuine platform bits — read
the file for the current list. `startKoin` cannot be shared: it needs `androidContext()` /
`androidLogger()` from koin-android. A new **feature** module is registered in `appModules()` in
`:presentation`, never here.

`AndroidPlatformModuleTest` is what covers these bindings. `AppGraphKoinTest` in `:presentation`
cannot see them, and `presentation/CLAUDE.md` explains the gap it leaves.

## Product flavors

Dimension `tier`. `dev` adds `applicationIdSuffix = ".dev"` and carries
`src/dev/kotlin/.../experiences/` — a personal Compose playground that is **not part of the
product**, shipping its own `experiencesModule` (`prod` has an empty stub of the same file). `prod`
gets release signing from `keystore.properties`.

Crashlytics is declared for every variant, but `src/dev/AndroidManifest.xml` sets
`firebase_crashlytics_collection_enabled=false`, which is what keeps the privacy policy's "build the
dev flavor for a telemetry-free app" claim true. Firebase **Analytics is not used**.

## Testing

`./gradlew :androidApp:testDevDebugUnitTest`. There is no instrumented source set here.

- `MainDispatcherRule` — **use it in every ViewModel test that touches `viewModelScope`**.
- **The MockK ViewModel tests live here, not in `:presentation`**, even though the ViewModels are in
  `presentation/src/main`: they sit in the same package and rely on MockK's JVM engine. That
  placement is deliberate — keep it unless you move the whole suite.
