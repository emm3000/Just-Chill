# ADR 024 — Kotlin Multiplatform returns: shared logic, native UI per platform

- **Status**: Accepted
- **Date**: 2026-09-28
- **Deciders**: Edgardo Muñoz
- **Supersedes**: [ADR 011](011-android-only-drop-the-kmp-build-and-the-ios-target.md) **in full**.
  Its Decision 3 guarantee (no `android.*` in `:core:domain`) survives as `commonMain`'s classpath;
  the `compileKotlinIos*` gate leg it removed comes back (Decision 3).
- **Amends**: [ADR 015](015-feature-modules-over-layer-modules.md) — the feature module layout
  (Decision 5) and the module list (Decisions 2 and 6).

> Resumen (es): JustChill vuelve a ser Kotlin Multiplatform, pero solo en la lógica: casos de uso,
> repositorios, SQLDelight, el respaldo, los ViewModels y los formateadores se comparten; la UI es
> nativa en cada plataforma — Compose en Android, SwiftUI en iOS, con SKIE para la superficie Swift.
> No es Compose Multiplatform. ADR 011 te dejó escrito el precio de volver y hoy lo pagas a
> sabiendas: ≈530 archivos pasan a `commonMain`/`androidMain`, cada pantalla se escribe dos veces
> para siempre y el gate vuelve a compilar Kotlin/Native. A cambio, iOS no reescribe ni una regla.
> Primero sale `:core:presentation` (solo Android), luego cada módulo se convierte de la dependencia
> al consumidor, y el único simulador iOS se crea recién en la ola 6.

## Context

Verified on 2026-09-28 against trunk `aa8a7d3a`.

1. **ADR 011 dropped a motive and a layout, not code**: learning was gone, and ≈610 files sat in
   four source sets. The owner now wants iOS on ADR 005's terms — shared logic, SwiftUI, SKIE.
2. **Android-bound code outside `:androidApp` is three files in two modules**: `:core:database`'s
   `DatabaseDriver.kt` and `shared/SqliteExceptions.kt`; `:feature:profile`'s `ProfileEntries.kt`.
3. **The 14 feature ViewModels take only Compose-free symbols from `:core:ui`**: `mvi.MviViewModel`,
   `error.toUserMessage`, `format.*` (`centsToMoney`, `moneyCentsString`, `monthLabel`,
   `monthAbbrevLabel`, `isSavableAmount`, `formatNeutral`, `format`), `category.SelectableCategory` /
   `toSelectable`, `transaction.Catalog` / `categoriesOf`, and `loan.toUi` / `PersonBalanceUi` /
   `totalOwedFormatted` / `totalOwedIsPositive` / `owingNames`. `format/` is pure Kotlin: hardcoded
   es-PE tables over `kotlinx.datetime` and `kotlin.math`, no `java.text`.
4. **MockK is in 63 test files, 31 of them in `:core:domain`.** MockK does not run on Kotlin/Native.
5. **Toolchain**: Kotlin 2.4.20, SQLDelight 2.4.0, Koin BOM 4.2.2, supabase-kt BOM 3.8.0, Ktor 3.6.0
   (okhttp; `ktor-client-darwin` already in the catalog), kotlinx-datetime 0.8.0, multiplatform-settings
   1.3.0 already holding the preferences. SKIE 0.10.15 (Maven Central, 2026-09-25) declares Kotlin
   2.4.20. Xcode 27.0 with the iOS 27 simulator SDK; `simctl list runtimes` is empty.
6. **`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel` 2.11.0** is the multiplatform
   ViewModel at the version Android already uses; `viewModelScope` is common since 2.8.0.
7. **`com.android.kotlin.multiplatform.library`** is supported from AGP 8.10 / KGP 2.0. Its test
   source sets are `androidHostTest` (`withHostTest {}`) and `androidDeviceTest`
   (`withDeviceTest {}`), both off by default. Google's Compose Preview Screenshot Testing plugin
   does not support KMP library modules (its Known Issues).
8. **Nav3 is Google's Android-only artifact.** Routes implement its `NavKey`, so routes are Android.

## Decision

1. **Targets `android`, `iosArm64`, `iosSimulatorArm64`; UI native per platform** — Compose on
   Android, SwiftUI on iOS. Compose Multiplatform is rejected: with one maintainer no design-system
   reuse pays for it, and the pad's behaviours (ADRs 019–022) are Android-specific.
2. **`:core:presentation` ships first, Android-only, before any KMP plugin.** On `:core:domain`,
   lifecycle-viewmodel, coroutines and datetime; no Compose. It takes from `:core:ui`: `mvi/`
   (`MviViewModel`, `UiState`, `UiIntent`, `UiEffect`), `error/toUserMessage`, `format/`,
   `category/SelectableCategory` with `CategoryUi`, `transaction/TransactionUi` and `Catalog`, and
   `loan/PersonBalanceUi`. `:core:ui` and every feature depend on it; `checkModuleBoundaries` learns
   its role. Compose-freedom is a module boundary again (ADR 011 Decision 4 reversed): for
   `:core:presentation` now, for every ViewModel once Decision 5 puts it in `commonMain`, where
   `androidx.compose` does not resolve. `checkComposeFreeViewModels` stays as belt and braces.
3. **Convention plugins `justchill.kmp.library` and `justchill.kmp.feature`** over
   `com.android.kotlin.multiplatform.library`. Source sets: `commonMain`, `androidMain`, `iosMain`,
   `commonTest`, `androidHostTest`, plus `androidDeviceTest` where a device suite exists. The
   MockK/JUnit4 tests move to `androidHostTest` unchanged; `commonTest` is for new
   `kotlin.test`-only tests. The gate adds `compileKotlinIosSimulatorArm64` on every shared module,
   and each converted module registers `testAndroidHostTest` — ADR 011's silent-drop trap, reversed.
4. **Conversion order is dependency before consumer** (ADR 011 Decision 6 inverted, same reason: a
   `commonMain` cannot resolve a JVM-only or plain-Android artifact): `:core:domain`,
   `:core:presentation`, `:core:testing`, `:core:database`, `:core:backup`, then the seven
   features with a ViewModel; `:feature:onboarding` has none and no Koin module, so it stays
   `com.android.library`.
   `:core:database` gets a plain `provideSqlDriver` per platform source set (`NativeSqliteDriver` on
   iOS; no `expect`, since nothing in `commonMain` calls it) and `expect`/`actual` for
   `SqliteExceptions`; its `.sq`, `.sqm` and `databases/N.db` move unchanged, and the migration suite
   moves to `androidDeviceTest` on `justchill-api36`. `:core:backup` takes the darwin engine in
   `iosMain`. `:core:ui` stays `com.android.library`; `:androidApp` stays the application.
5. **ADR 015 amended: a feature splits by source set, not by module.** Still one screen family per
   module. `commonMain`: ViewModels, `UiState` / `UiIntent` / `UiEffect`, the `<feature>Module` Koin
   module. `androidMain`: screens, `@Serializable` routes, the `<feature>Routes` registry, nav
   entries. `RouteSerializationTest` stays in `:androidApp`. **The screenshot matrix leaves the
   feature**: `:feature:transaction`'s `@PreviewWindowEdges` tests and references move to
   `:androidApp/src/screenshotTest`, `justchill.screenshot` applies there, and the move lands before
   `:feature:transaction` converts, making the `internal` `AddTransactionScreenContent` reachable.
6. **`:shared` exports `JustChillKit`.** An umbrella KMP module exports domain, presentation,
   database, backup and the seven converted features as one framework, with SKIE 0.10.15. `iosMain` holds the
   actuals for the ports `androidApp/core/` implements today: preferences (multiplatform-settings
   over `NSUserDefaults`), `Dispatchers`, `UniqueIdProvider`, `BackgroundEvents` / `ResumeEvents`,
   session storage (Keychain). Koin starts from Swift through an `initKoin` entry.
7. **`iosApp/` is SwiftUI, one screen family at a time, in product order**: onboarding, movements
   list, capture pad, report, accounts, categories, loans, then auth and backup (Google sign-in
   through the iOS SDK feeding supabase-kt `signInWith(IDToken)`).
8. **One simulator, one runtime, from wave 6 only.** Exactly one iOS simulator device,
   `justchill-ios27`, on the iOS 27 runtime (`xcodebuild -downloadPlatform iOS`, ≈6–8 GB, once).
   Waves 1–5 compile and link only and create none. No per-wave, per-peer or extra-runtime
   simulators; a dispatch that needs one names this device. Amended: a two-device pool
   (`## Amendments (2026-09-29)`).
9. **One risk stays open: detekt over KMP source sets.** The Decision 3 ticket carries a spike line;
   this ADR does not guess. SKIE and the screenshot plugin are settled (Context 5 and 7).

## Waves

1. `:core:presentation` out of `:core:ui` (Decision 2), Android-only.
2. The KMP plugins, gate legs and detekt spike; `:core:domain`, `:core:presentation`, `:core:testing`.
3. `:core:database`, then `:core:backup`.
4. The screenshot matrix to `:androidApp`, then the seven converted features, one ticket each.
5. `:shared`, the iOS actuals, `JustChillKit` linked for `iosSimulatorArm64`.
6. `iosApp/` per Decision 7; `justchill-ios27` is created here, and its pool twin with it
   (`## Amendments (2026-09-29)`).

## Consequences

### Cheaper

- iOS shares every use case, repository, ViewModel, formatter and the snapshot format; a rule is
  fixed once for both platforms. A Compose import in a ViewModel is a compile error again.

### Dearer

- **Every screen is written twice, forever**, by one maintainer: Compose and SwiftUI.
- The source-set layout returns on ≈530 files: 626 under `core/` and `feature/`, minus `:core:ui`'s 94.
- The gate runs a Kotlin/Native compile per shared module on macOS with Xcode (the local gate's
  host already is), and Kotlin upgrades wait on SKIE's support window again.
- The `kotlin.jvm` marker pin in `build-logic/convention/build.gradle.kts` serves `:core:domain`'s
  stdlib; that module's conversion re-derives whether it stays.
- `androidApp/core/di/`'s platform-neutral Koin modules (`DataModule`, `BackupModule`,
  `SupabaseModule`, `SharedModule`) move to `:shared`'s `commonMain` in wave 5; each app keeps only
  its platform module and wiring. Copying them would be the drift this ADR prevents.

### Unchanged

- Android behaviour, the SQLDelight schema, ADR 015's invariants (`com.emm.data.db`, the preference
  files and keys, `com.emm.justchill.MainActivity`, the `applicationId`), the backup snapshot format,
  and the MockK suite on the JVM.

## Amendments (2026-09-29)

- **The feature wirings are shared too** (#526). The seven `wiring/<Feature>Wiring.kt` files are
  platform-neutral Koin and move to `:shared`'s `commonMain` as `featureWirings`, which
  `:androidApp`'s `appModules()` and iOS's `initKoin` both start. This supersedes "each app keeps
  only its platform module and wiring" in Consequences: each app keeps only its platform module,
  and a use case is bound once for both platforms.
- **Wave 6 runs on a two-device simulator pool** (#546). `justchill-ios27` and
  `justchill-ios27-b`, both iPhone 17 on the one iOS 27 runtime, one per peer, as the Android pool
  pairs `justchill-api36` with `justchill-api36-b`. The runtime's 7.5 GB is shared; each device
  costs ≈2.3 GB of disk and ≈2 GB of RAM booted. This supersedes "Exactly one iOS simulator
  device" and "No per-wave, per-peer or extra-runtime simulators" in Decision 8; one runtime
  still holds. Rejected: one device behind a lock, because both peers install bundle id
  `com.emm.justchill.ios`, so one install silently replaces the other and a shot can show the
  wrong build. Kotlin/Native simulator tests stay on `justchill-ios27`.
