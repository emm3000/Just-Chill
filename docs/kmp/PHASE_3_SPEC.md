# Phase 3 — `shared-ui` (Compose Multiplatform) — Execution Spec

> Companion to `MIGRATION_PLAN.md §Phase 3`. This is the sliced, gated
> execution plan derived from a full `:app` audit (2026-06-13).
> **Prime directive: Android build stays green at every slice.**

## Resolved decisions

1. **Module name** = `shared-ui` (typesafe accessor `projects.sharedUi`).
   Matches the wizard + the plan text. (Open decision #4 → closed.)
2. **Navigation** = **Option A** (defer iOS nav). Android keeps its current
   **stable** `androidx.navigation3:*`. The nav host (`Hh.kt`, `HhRoutes.kt`,
   `ObjectsRoutes.kt`) **stays in `:app`/androidApp**. Feature screens move to
   `shared-ui/commonMain` as **callback-driven** composables (already MVI:
   they take `state` + `onIntent`, no navigation3 imports). iOS builds its own
   nav in Phase 5; it may adopt the JetBrains nav3 CMP fork
   (`org.jetbrains.androidx.navigation3:navigation3-ui`, currently `alpha05`)
   when it matures. Rationale: the CMP nav3 UI artifact is alpha; swapping
   Android's working nav mid-migration violates the prime directive.

## What moves to `shared-ui/commonMain` vs stays in `:app`

### Moves → `shared-ui/commonMain`
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

| Current (`:app`) | `shared-ui/commonMain` |
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

Compose for `shared-ui` is driven by the `org.jetbrains.compose` plugin
(already in the catalog as `compose-multiplatform = 1.11.1`) + the
`kotlin-compose` compiler plugin. The Android `composeBom` is NOT used by
`shared-ui` (CMP versions its own Compose).

## Slices (each = one Android-green gate: `./gradlew assembleDevDebug`)

> **STATUS (updated 2026-06-13, after Slice 1):**
> - ✅ Slice 0 — scaffold + MVI + theme (commit `7d66e54`)
> - ✅ Slice 1 — transactions + de-JVM (commit `4b10bad`)
> - ⏳ Slices 2-8 below, **re-sequenced**. Lessons from Slice 1 baked in.
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
> **Reinforced gate (every slice):** `./gradlew :shared-ui:compileAndroidMain`
> (note: NOT `compileDebugKotlinAndroid` — the new android KMP plugin renamed it)
> + `:shared-ui:compileKotlinIosSimulatorArm64` (proves zero `java.*` leak)
> + `assembleDevDebug` + `:app:testDevDebugUnitTest` + `:shared-ui:testAndroidHostTest`.

### Slice 0 — module scaffold + foundation (LOW risk) — ✅ DONE (`7d66e54`)
- Create `shared-ui/build.gradle.kts`: `kotlin.multiplatform` +
  `android.kotlin.multiplatform.library` + `iosArm64()` + `iosSimulatorArm64()`
  + `org.jetbrains.compose` + `kotlin-compose`. Framework `baseName = "Shared"`,
  `isStatic = true`. Android `namespace = "com.emm.justchill.shared"`,
  `compileSdk = 37`, `minSdk = 28` (matches `:app`), JVM 17. Mirror the shape of
  `data/build.gradle.kts`. **Do NOT apply `google-services`/`crashlytics`.**
- `settings.gradle.kts`: `include(":shared-ui")`; enable
  `TYPESAFE_PROJECT_ACCESSORS` if not on.
- Add CMP catalog entries: `compose.components.resources`,
  `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` (KMP),
  `io.insert-koin:koin-compose`.
- Move `core/mvi/*` → `shared-ui/src/commonMain/kotlin/.../core/mvi/`. Swap the
  `androidx.lifecycle` import to the JetBrains KMP `lifecycle-viewmodel`.
- Move `core/theme/*` → `commonMain`. Move `res/font/*.ttf` →
  `shared-ui/src/commonMain/composeResources/font/`. Rewrite `EmmType` to use
  `Res.font.*` (CMP) instead of `Font(R.font.*)`.
- `:app` adds `implementation(projects.sharedUi)`, deletes its `core/mvi/` and
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
moved to `shared-ui/commonTest` (pure JUnit); MockK VM tests stayed in `:app`.

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

### Slice 6 — onboarding — ✅ DONE (`13473d0`)
Moved `onboarding/ManifestoScreen.kt` (single stateless callback-driven screen).
No VM, no Koin, no de-JVM, no hotspot — only `@Preview` param strip + import swap.
Done inline (no writer/reviewer ceremony — risk ~nil). Nav host `Hh.kt` already
imported it explicitly. Gate green.

### Slice 7 — auth/sync UI + profile
Move `auth/` screens (NOT `GoogleCredentialClient`/`ActivityGoogleSignInLauncher`
— those stay android), `profile/`. Hoist `AuthScreen` open-email to a callback.
`ic_google.xml` → `composeResources/drawable/`, use `Res.drawable.ic_google`.
Inject `appVersion`/`isDebug` via Koin platform module (drop `BuildConfig` from
commonMain). Move the remaining agnostic part of `hhModule` (feature wiring) — keep
nav/platform bits in `:app`. Platform Koin modules (`DbModule`, `SupabaseModule`,
`AuthModule`, `SyncModule`) STAY in `:app`. Gate green.

### Slice 8 — cleanup
Move remaining agnostic `hh/shared/` atoms/utils not already pulled forward. Drop
unused `ui-text-google-fonts`. Update detekt config + `CLAUDE.md` build commands if
the `:app` task names changed. Gate green.

## Cross-cutting guards
- **Package paths unchanged**: keep `com.emm.justchill.*` package names when
  files move modules, so `:app` import statements don't all churn. Only the
  Gradle module boundary changes.
- **MockK tests**: any moved VM tests that use MockK stay in `:app`
  `androidUnitTest` (MockK is JVM-only) until rewritten as `commonTest` fakes.
  Do NOT force MockK into `commonTest`.
- **`google-services` plugin**: NEVER applied to `shared-ui` (breaks native).
- **Per-slice commit**: one feature group per commit, each `assembleDevDebug`
  green, mirroring the slice-per-commit history of the redesign track.
- **No iOS run yet**: Phase 3 only proves Android still builds from `shared-ui`.
  iOS first run is Phase 5.

## Done-when
All shared UI lives in `shared-ui`; `:app` holds only the nav host + platform
Koin modules + MainActivity/EmmApp + launcher resources; `assembleDevDebug`
green; the running Android app is visually + behaviorally identical to pre-Phase-3.
