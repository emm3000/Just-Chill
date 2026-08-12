# Phase 3 — `ui-android` (Compose Multiplatform) — Execution Spec

> Companion to `MIGRATION_PLAN.md §Phase 3`. This is the sliced, gated
> execution plan derived from a full `:app` audit (2026-06-13).
> **Prime directive: Android build stays green at every slice.**
>
> ⚠️ **HISTORICAL — Phase 3 is done and some decisions below were later reversed.**
> Read `docs/archive/kmp/ORCHESTRATION.md` (Carry-forward decisions / landmines) for what is true
> today. Specifically:
> - **Decision 2 (Navigation = Option A) was REVERSED** by slice F (`186d3b6`).
>   There is no longer a per-platform nav host and `Hh.kt` no longer exists: both
>   platforms run ONE commonMain `hh/shared/AppNavHost.kt` on the JetBrains
>   nav3-UI port. The predictive-back risk that justified Option A turned out to
>   be nil — the app never opted into the gesture.
> - **`:app` means `:androidApp`** everywhere below (renamed in Phase 4).
> - The commonMain **`:domain`-only rule was reversed** by slice H (`56314ba`):
>   `ui-android/commonMain` now depends on `:data` so the Koin wiring exists once.
> Everything after §"Resolved decisions" is a per-slice execution record — accurate
> as history, not as a description of the current tree.

## Resolved decisions

1. **Module name** = `ui-android` (typesafe accessor `projects.uiAndroid`).
   Matches the wizard + the plan text. (Open decision #4 → closed.)
2. **Navigation** = **Option A** (defer iOS nav). Android keeps its current
   **stable** `androidx.navigation3:*`. The nav host (`Hh.kt`, `HhRoutes.kt`,
   `ObjectsRoutes.kt`) **stays in `:app`/androidApp**. Feature screens move to
   `ui-android/commonMain` as **callback-driven** composables (already MVI:
   they take `state` + `onIntent`, no navigation3 imports). iOS builds its own
   nav in Phase 5; it may adopt the JetBrains nav3 CMP fork
   (`org.jetbrains.androidx.navigation3:navigation3-ui`, currently `alpha05`)
   when it matures. Rationale: the CMP nav3 UI artifact is alpha; swapping
   Android's working nav mid-migration violates the prime directive.

## What moves to `ui-android/commonMain` vs stays in `:app`

### Moves → `ui-android/commonMain`
- **MVI base** (`core/mvi/`): `UiState`, `UiIntent`, `UiEffect`, `MviViewModel`.
  All portable once `lifecycle-viewmodel` is the JetBrains KMP artifact. No
  `android.*` imports.
- **Theme** (`core/theme/`): `Color`, `EmmColors`, `EmmRadii`, `EmmSpacing`,
  `EmmType`, `Theme`, `Type`. `EmmType` swaps `Font(R.font.*)` → CMP
  `Res.font.*`.
- **Feature screens + VMs + UiState/Intent/Effect + helpers** for every feature
  under `hh/<feature>/` EXCEPT the nav-host pieces in `hh/shared/`.
- **Platform-agnostic Koin modules**: `hhModule`, `categoryModule`,
  `accountModule`, `transactionModule`.
- **Shared UI atoms/utils** in `hh/shared/` that are not nav: `UiStrings`,
  `CurrencyFormat`, `MonthLabels`, `EmmDropDown`, `EmmPrimaryButton`,
  `LabelTextField`, `Filters`, `Utils`, `SyncEventsHandler`, etc.
- **Resources**: `res/font/*.ttf` → `composeResources/font/`;
  `res/drawable/ic_google.xml` → `composeResources/drawable/`.

### Stays in `:app` / androidApp (platform-specific)
- **Nav host**: `Hh.kt`, `HhRoutes.kt`, `ObjectsRoutes.kt` (Option A).
- **Platform Koin modules**: `coreModule` (Context/SharedPreferences/
  `CurrentActivityHolder`), `dbModule` (AndroidSqliteDriver), `supabaseModule`
  (`BuildConfig`), `authModule` (`BuildConfig` + `CredentialManager` +
  `ActivityGoogleSignInLauncher`), `syncModule` (`ProcessLifecycleOwner`).
- **`core/preferences/AppPreferences`** (SharedPreferences), `core/sync/
  ProcessResumeEvents`, `core/sync/AppPreferencesSyncCursorStore`.
- **MainActivity/EmmApp**, `enableEdgeToEdge`, launcher icons, XML themes.

## Platform hotspots inside UI (need handling when their feature moves)

| Hotspot | Files | Strategy |
|---|---|---|
| Share intent (`ACTION_SEND`) | `ReportScreen` | Hoist to a callback `onShare(text)` provided by the nav host in `:app`; OR `expect/actual` a `Sharer`. Screen takes a lambda. |
| Open-email intent | `AuthScreen` | Same — `onOpenEmailApp()` callback from nav host. |
| Export/import (`ActivityResultContracts`, `contentResolver`) | `Hh.kt` | Already in the nav host → stays in `:app`. VM stays Stream-based (Android-free). |
| `BuildConfig.VERSION_NAME` | `ProfileScreen`, `ProfileViewModel` | Inject `appVersion: String` via Koin (platform module provides it). No `BuildConfig` in commonMain. |
| `BuildConfig.DEBUG` guard | `ProfileScreen` | Inject `isDebug: Boolean` via Koin platform module. |
| `painterResource(R.drawable.ic_google)` | `AuthScreen` | CMP `Res.drawable.ic_google` from `composeResources`. |
| `Font(R.font.*)` | `EmmType` | CMP `Res.font.*` from `composeResources`. |

## Dependency mapping (Jetpack → Compose Multiplatform)

| Current (`:app`) | `ui-android/commonMain` |
|---|---|
| `androidx.compose.ui:ui` / `ui-graphics` | `compose.ui` (CMP plugin DSL) |
| `androidx.compose.material3:material3` | `compose.material3` |
| `androidx.compose.material:material-icons-extended` | `compose.materialIconsExtended` |
| `androidx.lifecycle:lifecycle-viewmodel-compose` | `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` |
| `io.insert-koin:koin-androidx-compose` | `io.insert-koin:koin-compose` (commonMain) |
| `io.coil-kt:coil-compose` (Coil 2) | `io.coil-kt.coil3:coil-compose` (only if needed; verify usage) |
| `androidx.compose.ui:ui-text-google-fonts` | **DROP** — declared but unused |
| `androidx.activity:activity-compose` | stays in androidApp |
| `androidx.navigation3:*`, `lifecycle-viewmodel-navigation3` | stays in androidApp (Option A) |
| Compose Resources (fonts/drawables) | `compose.components.resources` |

Compose for `ui-android` is driven by the `org.jetbrains.compose` plugin
(already in the catalog as `compose-multiplatform = 1.11.1`) + the
`kotlin-compose` compiler plugin. The Android `composeBom` is NOT used by
`ui-android` (CMP versions its own Compose).

## Slices (each = one Android-green gate: `./gradlew assembleDevDebug`)

> **STATUS (updated 2026-06-13): ✅ PHASE 3 COMPLETE.**
> All slices 0–8b done (full ledger in `docs/archive/kmp/ORCHESTRATION.md`). All shared UI lives in
> `ui-android` commonMain; `:app` holds only the nav host + platform Koin modules +
> MainActivity/EmmApp. `assembleDevDebug` + iOS compile + all tests green.
> **Post-phase cleanup also done** (not slices, separate commits):
> - `8afe740` — dedup `Clock` binding (keep commonMain `sharedModule` provider).
> - `85fa2b5` + `37bc4fd` — migrate `@Preview` to the CMP 1.10+ multiplatform
>   `androidx.compose.ui.tooling.preview.Preview` (dep `org.jetbrains.compose.ui:ui-tooling-preview`)
>   + add `org.jetbrains.compose.ui:ui-tooling` (`androidRuntimeClasspath`) so previews RENDER in the IDE.
> - `f3b0219` — migrate `compose.*` plugin accessors → version-catalog `libs.compose.*`
>   deps (material3 pinned `1.11.0-alpha07`, material-icons-extended `1.7.3` — both versioned
>   independently of the umbrella CMP version).
> - `4d1305a` — sweep `invisibleToUser`→`hideFromAccessibility`, `MenuAnchorType`→`ExposedDropdownMenuAnchorType`.
> **Left deliberately:** `BackHandler` (works; migration needs a new nav-event dep + API rewrite),
> `androidLibrary {}` block (the official CMP wizard still uses it), AGP-10 option warnings.
> **NEXT: Phase 4** — split `:app` → `androidApp` thin entry point (still Android-only).
>
> ---
> _Historical lessons below (kept for reference — Slice 1 set the playbook):_
>
> **Dependency-closure lesson (CRITICAL — applies to every remaining slice):**
> A feature CANNOT move alone. `commonMain` cannot import from `:app`, so moving
> a feature drags in **every shared symbol it transitively consumes**. Slice 1
> was planned as ~30 files; it landed at **~62** because transactions pulled in
> `core/ui/atoms`, `core/format/MoneyFormatter`, `core/error/DomainExceptionExt`,
> `hh/shared/{CurrencyFormat,Utils}`, plus parts of `category/` and `account/`.
> **Before each slice: map the full transitive closure FIRST** (`rg` the feature's
> imports), and budget for: (a) `internal → public` flips on any symbol `:app`
> still consumes cross-module; (b) iOS-only API swaps (see hotspots below);
> (c) hand-rolled de-JVM of any `java.*`. Full gotcha log in engram
> `kmp/phase-3/slice-1-de-jvm`.
>
> **De-JVM playbook (established in Slice 1, reuse verbatim):** app is
> Spanish-only → NO `expect/actual`, NO locale machinery. Hand-roll in pure
> commonMain: `java.time.*`→`kotlinx-datetime`; localized Spanish dates→hardcoded
> month/day tables (`hh/shared/SpanishDateFormat.kt`); `DecimalFormat`→
> `NumberFormatEs.kt`; `java.text.Normalizer`→`SpanishSearch.kt`; `java.util.UUID`
> →`kotlin.uuid.Uuid`; `koin.androidx.compose.koinViewModel`→`koin-compose-viewmodel`.
> Strip CMP-incompatible `@Preview` params; `LocalConfiguration`→`LocalWindowInfo`
> +`LocalDensity`. Golden test (`SpanishFormatGoldenTest`) guards Spanish output.
>
> **Reinforced gate (every slice):** `./gradlew :ui-android:compileAndroidMain`
> (note: NOT `compileDebugKotlinAndroid` — the new android KMP plugin renamed it)
> + `:ui-android:compileKotlinIosSimulatorArm64` (proves zero `java.*` leak)
> + `assembleDevDebug` + `:app:testDevDebugUnitTest` + `:ui-android:testAndroidHostTest`.

### Slice 0 — module scaffold + foundation (LOW risk) — ✅ DONE (`7d66e54`)
- Create `ui-android/build.gradle.kts`: `kotlin.multiplatform` +
  `android.kotlin.multiplatform.library` + `iosArm64()` + `iosSimulatorArm64()`
  + `org.jetbrains.compose` + `kotlin-compose`. Framework `baseName = "Shared"`,
  `isStatic = true`. Android `namespace = "com.emm.justchill.shared"`,
  `compileSdk = 37`, `minSdk = 28` (matches `:app`), JVM 17. Mirror the shape of
  `data/build.gradle.kts`. **Do NOT apply `google-services`/`crashlytics`.**
- `settings.gradle.kts`: `include(":ui-android")`; enable
  `TYPESAFE_PROJECT_ACCESSORS` if not on.
- Add CMP catalog entries: `compose.components.resources`,
  `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (KMP),
  `io.insert-koin:koin-compose`.
- Move `core/mvi/*` → `ui-android/src/commonMain/kotlin/.../core/mvi/`. Swap the
  `androidx.lifecycle` import to the JetBrains KMP `lifecycle-viewmodel`.
- Move `core/theme/*` → `commonMain`. Move `res/font/*.ttf` →
  `ui-android/src/commonMain/composeResources/font/`. Rewrite `EmmType` to use
  `Res.font.*` (CMP) instead of `Font(R.font.*)`.
- `:app` adds `implementation(projects.uiAndroid)`, deletes its `core/mvi/` and
  `core/theme/` copies, re-imports from the shared package (keep the SAME package
  path `com.emm.justchill.core.{mvi,theme}` so imports across `:app` don't churn).
- **Gate**: `./gradlew :data:compileDebugKotlinAndroid` + `assembleDevDebug`
  green. App still runs, theme + fonts render identically.

### Slice 1 — transactions (add/edit/list) — ✅ DONE (`4b10bad`)
Moved `transaction/`, `seetransactions/`, `transactionModule`. Reality: ~62 files
(dependency closure). De-JVM'd `DateUtils`, `DayGroup`, `SeeTransactionsViewModel`,
`DatePickerSheet`, `CategoryFilterSheet`, `CentsFormatter`, `MoneyFormatter`,
`AmountHero`, `IconsAll`, etc. Co-moved to commonMain (originally other slices):
`core/ui/atoms`, `core/format`, `core/error/DomainExceptionExt`,
`category/{CategoryColor,ColorsAll,IconCatalog,IconsAll}`, `account/AccountPalette`,
`hh/shared/{CurrencyFormat,Utils}` + new `hh/shared/{SpanishDateFormat,NumberFormatEs,
SpanishSearch}.kt`. `HhModule` stays in `:app` (nav host). `CentsFormatterTest`
moved to `ui-android/commonTest` (pure JUnit); MockK VM tests stayed in `:app`.

### Slice 2 — categories — ✅ DONE (`1f35f32`)
Moved category screens/VMs + `categoryModule` (use cases only; `DefaultCategoryRepository`
binding moved to `:app` `HhModule`). Co-moved `components/EmmTextInput.kt`. Established
the **commonMain depends on `:domain` only, NOT `:data`** Koin-split pattern.

### Slice 3 — accounts — ✅ DONE (`5701f95`, writer+reviewer)
Moved account screens/VMs + `accountModule` (use cases only; repo binding → `HhModule`).
Co-moved `components/{EmmButton,EmmButtonVariant}.kt`.

### Slice 4 — recurring — ✅ DONE (`591cbcc`, writer+reviewer)
Moved all 15 recurring files. No separate Koin module — VMs/use-cases/repo binding
already in `:app` `HhModule` (untouched). Co-moved `EmmCard`, `Pill`, `PillTone`,
`EmmSwitch`. No de-JVM needed.

### Slice 5 — home + report — ✅ DONE (`b82f128`, writer+reviewer)
Moved home (5) + report (23). Hoisted `ReportScreen` share intent (`ACTION_SEND`) to
an `onShareText(String)` callback wired in the nav host (`Hh.kt`). De-JVM'd
`ReportFormat` + a hidden `DecimalFormat` leak in `MoneyInline` via `NumberFormatEs`
(added `integerRounded` with HALF_EVEN to match java's es-PE rounding — reviewer
brute-forced 100M cent values, zero mismatches). `@PreviewLightDark`→`@Preview` (15).
Co-moved `core/ui/atoms/{MonthSelector,MoneyInline,Segmented}`, `hh/shared/MonthLabels`.

### Slice 6 — onboarding — ✅ DONE (`66df00e`)
Moved `onboarding/ManifestoScreen.kt` (single stateless callback-driven screen).
No VM, no Koin, no de-JVM, no hotspot — only `@Preview` param strip + import swap.
Done inline (no writer/reviewer ceremony — risk ~nil). Nav host `Hh.kt` already
imported it explicitly. Gate green.

### Slice 7 — auth + profile (SPLIT into 7a / 7b — coupling too complex for one review)

#### Slice 7a — auth — ✅ DONE (`eed7778`, writer+reviewer)
Moved `AuthEffect/Intent/UiState/ViewModel/Screen` + the `GoogleSignInLauncher`
interface to commonMain. **Extracted** `GoogleSignInResult` (sealed) to commonMain;
`GoogleCredentialClient` + `ActivityGoogleSignInLauncher` (split to its own file)
STAY in `:app`, implement the common interface. Removed `android.util.Log` from the
VM. Hoisted open-email intent → `onOpenEmailApp()` callback in nav host. `ic_google.xml`
→ `composeResources/drawable/`, `Res.drawable.ic_google`. Swapped Android `BackHandler`
→ CMP `androidx.compose.ui.backhandler.BackHandler` (+ `ui-backhandler` dep,
`@OptIn(ExperimentalComposeUiApi)`). `authModule` stays in `:app`. **Reviewer caught a
RED gate** the writer falsely reported green: co-moving `EmmSnackbar.kt` (holds
`internal highlightQuoted`) orphaned `HighlightQuotedTest.kt` in `:app` (can't see a
`ui-android` internal). Fixed by moving the test to `commonTest` (junit→kotlin.test).
**Lesson: co-moving a file with an `internal` symbol breaks its test if the test
stays behind — move the test too.**

#### Slice 7b — profile — ✅ DONE
Moved `profile/` (`ProfileScreen/ViewModel/UiState/Intent/Effect`, `DeleteAccountDialog`,
`PrivacyPolicyScreen`, `RetryPill`) to `ui-android` commonMain. Resolved work:
- **Export contract**: `ProfileIntent.ExportToStream(OutputStream)` → `ExportRequested` (object).
  VM generates the JSON and emits it via new `ProfileEffect.ExportReady(json)`; the nav host
  owns the SAF write (and now the disk-space `ExportFailed` hint, which used to live in the VM —
  the VM only surfaces domain errors from generation as `ShowError`). Symmetric with `ImportJson(String)`.
- **BuildConfig**: `VERSION_NAME` → `appVersion: String` injected into `ProfileViewModel` via a
  `named("appVersion")` Koin binding in `coreModule` (platform layer). `DEBUG`/`VERSION_NAME` in
  the screen → `isDebug`/`appVersion` composable params passed by the nav host. No `BuildConfig` in commonMain.
- **De-JVM dates**: `SimpleDateFormat("d MMM, HH:mm", es)` → new `SpanishDateFormat.dayShortMonthTime`
  helper + `Instant.fromEpochMilliseconds(...).toLocalDateTime(...)`.
- **SyncOrchestrator coupling**: ProfileViewModel needed `SyncOrchestrator` (lives in `:app`, depends
  on Android `AppPreferences`). **Extracted** a narrow `SyncController` interface (`status` +
  `requestSync`) + moved `SyncStatus` (pure data class) to commonMain; `SyncOrchestrator` STAYS in
  `:app`, implements `SyncController`, bound via `bind SyncController::class`. De-JVM'd the orchestrator
  in passing (`@Volatile` → `kotlin.concurrent.Volatile`, `System.currentTimeMillis()` → `Clock.System`).
- `ProfileViewModel` wired with an explicit `viewModel { }` block (qualified `appVersion` can't go
  through the constructor-DSL); `clock` omitted to use its `Clock.System` default.

Verified: `assembleDevDebug` green, `:app` + `ui-android` host unit tests green, `commonMain`
compiles for `iosSimulatorArm64` (no JVM leak). MockK VM tests stay in `:app` `test/` (same package).

> **Deferred to Slice 8**: the "move the remaining agnostic part of `hhModule`" sweep is a
> cross-feature DI cleanup (home/report/recurring/seetransactions wiring), not profile-specific.
> Kept out of 7b to preserve the one-feature-per-commit, reviewable-slice discipline.
Platform Koin modules (`DbModule`, `SupabaseModule`, `AuthModule`, `SyncModule`) STAY in `:app`.

### Slice 8 — cleanup (SPLIT into 8a / 8b — coupling too complex for one review, mirrors 7a/7b)

#### Slice 8a — agnostic shared atoms + orphan deletion + fonts drop — ✅ DONE
Moved the remaining agnostic `hh/shared/` atoms/utils to `ui-android/commonMain`:
`LabelTextField`, `UiStrings` (as-is), `EmmDropDown`, `EmmPrimaryButton`, `Filters`
(`@PreviewLightDark`/`@Preview(showBackground=true)` → CMP param-less
`org.jetbrains.compose.ui.tooling.preview.Preview`, preview functions kept), and
`DefaultUniqueIdProvider` (de-JVM'd `java.util.UUID` → `kotlin.uuid.Uuid` with
`@file:OptIn(ExperimentalUuidApi::class)`, matching the domain precedent). Its Koin
binding (`factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class`) STAYS in
`:app` `HhModule` (the explicit `import com.emm.justchill.hh.shared.DefaultUniqueIdProvider`
resolves cross-module). Deleted orphans `NowProvider`/`DefaultNowProvider` (zero usage,
unbound). Dropped the unused `ui-text-google-fonts` dependency + its catalog `[versions]`
+ `[libraries]` entries. After 8a, `:app` `hh/shared/` holds only the nav-host/platform
pieces: `Hh.kt`, `HhRoutes.kt`, `ObjectsRoutes.kt`, `SyncEventsHandler.kt`. Gate green
(incl. iOS compile — zero `java.*` leak). No detekt/`CLAUDE.md` task-name changes.

#### Slice 8b — hhModule DI sweep — ✅ DONE
Split `:app`'s `hhModule` into `commonMain` per-feature Koin modules. Domain use cases +
ViewModels (which depend on `:domain` repository INTERFACES, not `Default` impls) moved to
`commonMain`; `:data` repository binds + `LocalDataSource`s stayed in `:app` `hhModule`.
New `commonMain` modules (`ui-android/.../hh/di/`): `reportModule` (5 report use cases +
`ReportViewModel`), `recurringModule` (7 recurring use cases + `RecurringMovementsViewModel`
+ explicit `AddEditRecurringMovementViewModel` param block), `homeModule` (`HomeViewModel`;
its use cases live in transaction/recurring/dbModule and resolve globally), `seetransactionsModule`
(`SeeTransactionsViewModel`), `profileModule` (explicit `ProfileViewModel` block — qualified
`appVersion` from `:app` `coreModule`, `clock` omitted to use its default), `backupModule`
(`ExportDataUseCase` + `ImportDataUseCase`), and `sharedModule` (cross-cutting: `DateAndTimeCombiner`,
`DefaultUniqueIdProvider bind UniqueIdProvider`, `TimeZone`, `Clock`). Extended existing
`categoryModule`/`accountModule`/`transactionModule` with their ViewModel registrations
(explicit param blocks preserved verbatim). `:app` `hhModule` now holds ONLY the 6 repository
binds + 5 `LocalDataSource`s. All 7 new modules registered in `EmmApp.startKoin { modules(...) }`.
Binding-exactly-once audit: every use case (per `factoryOf` uniqueness check), all 12 ViewModels,
6 repo binds, 5 data sources, and the cross-cutting factories each appear EXACTLY ONCE — except
the pre-existing `Clock` duplicate (`single<Clock>` in `dbModule` + `factory<Clock>` in `sharedModule`,
both `Clock.System`; this coexistence predates 8b and Koin last-wins makes it behavior-identical).
Gotcha: `factoryOf(::X) { bind<Y>() }` uses `org.koin.core.module.dsl.bind` (lambda form), while
`X bind Y::class` uses `org.koin.dsl.bind` (infix) — `hhModule` needs the dsl variant, `sharedModule`
the infix one. Gate green (incl. iOS compile — zero `java.*` leak). This CLOSES Phase 3.

## Cross-cutting guards
- **Package paths unchanged**: keep `com.emm.justchill.*` package names when
  files move modules, so `:app` import statements don't all churn. Only the
  Gradle module boundary changes.
- **MockK tests**: any moved VM tests that use MockK stay in `:app`
  `androidUnitTest` (MockK is JVM-only) until rewritten as `commonTest` fakes.
  Do NOT force MockK into `commonTest`.
- **`google-services` plugin**: NEVER applied to `ui-android` (breaks native).
- **Per-slice commit**: one feature group per commit, each `assembleDevDebug`
  green, mirroring the slice-per-commit history of the redesign track.
- **No iOS run yet**: Phase 3 only proves Android still builds from `ui-android`.
  iOS first run is Phase 5.

## Done-when
All shared UI lives in `ui-android`; `:app` holds only the nav host + platform
Koin modules + MainActivity/EmmApp + launcher resources; `assembleDevDebug`
green; the running Android app is visually + behaviorally identical to pre-Phase-3.
