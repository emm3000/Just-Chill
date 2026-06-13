# KMP / Compose Multiplatform Migration Plan — JustChill

> **Goal**: Make JustChill an Android + iOS app sharing one Kotlin/Compose
> codebase, **without ever breaking the Android build**. Migration is
> **Android-first, iOS-additive**: every step keeps Android compiling green;
> iOS is added as *new targets*, never at the cost of what already works.
>
> **Status**: PLANNING — not started. No gradle touched yet.
> **Date**: 2026-06-12
> **Owner**: @emm

---

## 0. Guiding principles

1. **Android green at every PR.** The gate after each phase is:
   ```bash
   ./gradlew assembleDevDebug   # MUST pass before the PR merges
   ./gradlew :domain:test       # and the relevant unit tests
   ```
   If it's red, the PR is not done. No exceptions.

2. **Add targets, don't remove capability.** Converting a module from
   `kotlin.jvm` / `android.library` to `kotlin.multiplatform` *adds* an iOS
   target next to the existing Android/JVM one. Android consumers keep
   resolving the same `androidTarget` variant. Nothing they depend on disappears.

3. **Platform code goes in platform source sets, never deleted.**
   `AndroidSqliteDriver`, OkHttp, `java.time`, Android exception types — all
   stay available to Android via `androidMain`. iOS gets a parallel `iosMain`
   implementation behind `expect/actual`.

4. **Incremental, not Big Bang.** Each phase below is one (or a few) PR(s),
   independently mergeable, each leaving a working Android app.

5. **The JetBrains wizard project is a *config reference*, not a template to
   clone.** We keep our 3-module Clean Architecture (which is BETTER than the
   wizard's single `shared`). We copy the wizard's `iosApp/` Xcode scaffold and
   its `shared/build.gradle.kts` patterns. Reference unzipped at
   `/tmp/kp_extract/KotlinProject` (re-unzip `~/Downloads/KotlinProject.zip` if gone).

---

## 1. Target structure (destination)

Current → Target. We do **not** collapse into a single `shared` module.

```
JustChill/
├── domain/        KMP library (pure Kotlin, commonMain only)
│                    targets: androidTarget + iosArm64 + iosSimulatorArm64
├── data/          KMP library (androidLibrary{} DSL + ios targets)
│                    commonMain: repos, SQLDelight, Supabase, sync logic
│                    androidMain: AndroidSqliteDriver, OkHttp engine
│                    iosMain:     NativeSqliteDriver, Darwin engine
├── shared-ui/     NEW — Compose Multiplatform UI + ViewModels + MVI + Koin modules
│                    (extracted from today's :app)
├── androidApp/    Thin Android entry point (was :app, gutted)
│                    Activity, Application, Firebase, Crashlytics,
│                    Google Sign-In (Credential Manager), manifest, Koin-android bootstrap
└── iosApp/        NEW — Xcode project, links the `Shared` framework,
                     SwiftUI shell hosting Compose via MainViewController()
```

**Why this variant (shared Compose UI, not native SwiftUI):** the wizard's
default and our chosen path is **UI compartida** — one Compose codebase for both
platforms. iOS hosts Compose inside a minimal `UIViewControllerRepresentable`
SwiftUI shell (exactly what `KotlinProject/iosApp/.../ContentView.swift` does).
If we later want native SwiftUI screens, that's the *other* variant
(`sharedLogic` + `sharedUI`) and a separate future decision — out of scope here.

---

## 2. What we already verified (codebase audit, 2026-06-12)

### domain — TRIVIAL ✅
- **Zero** `java.*` / `javax.*` imports.
- **Zero** `android.*` imports.
- Deps: `kotlinx-coroutines-core`, `kotlinx-datetime` — both KMP-native.
- Tests use **MockK** (JVM-only) → must stay in an Android/JVM test source set.

### data — THE REAL WORK ⚠️
Android-specific surfaces found (each needs `expect/actual` or a KMP swap, NOT deletion):

| # | Surface | Files | Action |
|---|---------|-------|--------|
| D1 | `AndroidSqliteDriver` + `Context` + `SupportSQLiteDatabase` | `Providers.kt` | `expect` driver factory; `androidMain` = AndroidSqliteDriver(Context), `iosMain` = NativeSqliteDriver |
| D2 | `java.time.Instant` / `OffsetDateTime` | `SyncCursorUtils.kt`, `BaseTableSync.kt`, `DefaultSyncRepository.kt` | Replace with `kotlinx-datetime` (already a project dep) |
| D3 | `System.currentTimeMillis()` | `DefaultBackupRepository.kt:58` | Replace with `kotlinx.datetime.Clock.System.now()` |
| D4 | `android.database.sqlite.SQLiteConstraintException` / `SQLiteException` | `SafeCall.kt`, `TransactionTableSync.kt`, `RecurringMovementTableSync.kt`, `CategoryTableSync.kt`, `AccountTableSync.kt` | **Trickiest.** Native SQLDelight throws different exceptions. `expect/actual` the exception→DomainException mapping, or catch SQLDelight's own common exception type |
| D5 | Ktor engine `ktor-client-okhttp` | `data/build.gradle.kts:57` | OkHttp is JVM/Android-only. iOS needs `ktor-client-darwin`. Split by source set |
| D6 | Dead Android deps | `data/build.gradle.kts:42-44` | `androidx.core.ktx`, `appcompat`, `material` are **unused** — drop them (only `androidx.sqlite` SupportSQLiteDatabase is real, comes with the driver) |

KMP-ready already (no change to the libs, just where they're declared):
- **Supabase** (`supabase-bom`, `supabase-auth-kt`, `supabase-postgrest-kt`) — has KMP artifacts ✅
- **SQLDelight** `.sq` files (`accounts/categories/recurring_movements/transactions.sq`) — move `data/src/main/sqldelight/` → `data/src/commonMain/sqldelight/`, package stays `com.emm.data`
- `kotlinx-serialization-json`, `kotlinx-coroutines` — KMP ✅

### app — BIGGEST SURFACE ⚠️⚠️
- **196 Kotlin files**, **15 screens**, **14 ViewModels**, **9 Koin modules**
  (`CoreModule`, `AuthModule`, `SupabaseModule`, `AccountModule`,
  `TransactionModule`, `SyncModule`, `DbModule`, `CategoryModule`, `HhModule`).
- **Jetpack Compose** (Android-only artifacts) → must swap to **Compose Multiplatform**.
- **Android-only, stays in `androidApp`**: Firebase BOM + Crashlytics,
  `google-services` plugin, Google Sign-In (`google-identity-googleid` =
  Credential Manager), Google Fonts (`androidx.ui.text.google.fonts`).
- MVI base (`app/core/mvi/MviViewModel`) → moves to `shared-ui` commonMain
  (StateFlow/Flow/coroutines are all KMP). `androidx.lifecycle.viewmodel`
  has KMP artifacts now (`lifecycle-viewmodel-compose` multiplatform).

---

## 3. Phased plan (each phase = Android-green gate)

### Phase 0 — Scaffolding & version catalog (Android untouched) — LOW RISK
**Scope:** prepare the ground. No module converted yet.
- [ ] Add KMP plugins to `gradle/libs.versions.toml`:
      `kotlinMultiplatform`, `androidMultiplatformLibrary` (AGP KMP lib plugin),
      `composeMultiplatform`, `composeCompiler`.
- [ ] Add iOS-only libs (declared, not yet used): `ktor-client-darwin`,
      `sqldelight-native-driver`.
- [ ] Confirm versions are mutually compatible: Kotlin 2.3.21, Compose
      Multiplatform matching, AGP 9.2.1, SQLDelight 2.3.2, Supabase BOM, Koin 4.2.
- [ ] Copy `iosApp/` Xcode scaffold from the wizard into the repo root
      (NOT wired to gradle yet — `settings.gradle.kts` unchanged; it's an Xcode
      project, not a gradle module).
- [ ] Decide JVM target: keep **17** (project standard), not the wizard's 11.

**Gate:** `./gradlew assembleDevDebug` green (nothing functional changed).

---

### Phase 1 — `domain` → KMP — LOW RISK
**Scope:** convert the cleanest module first to validate the KMP toolchain on the Mac.
- [ ] Rewrite `domain/build.gradle.kts`: `java-library` + `kotlin.jvm`
      → `kotlin.multiplatform` with `androidLibrary{}` (KMP DSL) +
      `iosArm64()` + `iosSimulatorArm64()`. Framework `baseName = "Shared"` is
      configured at the `shared-ui` level later; domain just exposes its API.
- [ ] Move `domain/src/main/kotlin` → `domain/src/commonMain/kotlin`.
- [ ] Move tests: **MockK tests stay in `androidHostTest` (JVM)**, NOT
      `commonTest`. `commonTest` only for pure `kotlin-test`. This keeps the
      existing domain test suite green on Android.
- [ ] Verify `:data` and `:app` still resolve `domain` (they consume its
      `androidTarget` variant — same Kotlin, recompiled).

**Risk points:** consumer resolution (Android modules must pick domain's
android variant — guaranteed by `androidLibrary{}`). MockK in commonTest would
fail to compile for iOS → that's why tests stay in the Android source set.

**Gate:** `./gradlew assembleDevDebug` + `./gradlew :domain:test` green.
**Bonus gate:** `./gradlew :domain:compileKotlinIosArm64` (proves iOS target builds).

---

### Phase 2 — `data` → KMP — HIGH RISK (split into 2a–2e PRs)
The meat. Do as **several small PRs**, each Android-green.

**2a — Module skeleton + move sources**
- [ ] Rewrite `data/build.gradle.kts` to `kotlin.multiplatform` +
      `androidLibrary{}` + ios targets. Keep SQLDelight + serialization plugins.
- [ ] Move `data/src/main/kotlin` → `commonMain/kotlin`.
- [ ] Move `data/src/main/sqldelight/` → `commonMain/sqldelight/` (package
      `com.emm.data` unchanged; `EmmDatabaseData` config unchanged).
- [ ] Drop dead deps D6 (`core.ktx`, `appcompat`, `material`).
- [ ] Temporarily keep Android-specific code compiling by putting it in
      `androidMain` as-is (driver, exceptions, java.time) BEFORE adding iOS.
- **Gate:** Android green. (iOS targets may not compile yet — acceptable
  *only* if Android assemble passes; iOS turns on in 2b–2e.)

**2b — SqlDriver `expect/actual` (D1)**
- [ ] `commonMain`: `expect fun provideDatabaseDriver(...): SqlDriver`.
- [ ] `androidMain`: actual = `AndroidSqliteDriver(schema, context, ...)` —
      keeps the `Context` + callback logic from today's `Providers.kt`.
- [ ] `iosMain`: actual = `NativeSqliteDriver(schema, name)`.
- [ ] Koin `DbModule` (in app/shared) provides the driver per platform.
- **Gate:** Android green + `:data:compileKotlinIosArm64` green.

**2c — Time APIs → kotlinx-datetime (D2 + D3)**
- [ ] Replace `java.time.Instant`/`OffsetDateTime` in `SyncCursorUtils`,
      `BaseTableSync`, `DefaultSyncRepository` with `kotlinx.datetime` types.
      ⚠️ Watch cursor serialization format — the sync cursor string format must
      stay byte-identical so existing synced devices don't break (see
      `docs/adr/002`). Add a round-trip test.
- [ ] Replace `System.currentTimeMillis()` in `DefaultBackupRepository` with
      `Clock.System.now().toEpochMilliseconds()`.
- **Gate:** Android green + sync unit tests green + cursor format test green.

**2d — SQLite exception mapping `expect/actual` (D4) — TRICKIEST**
- [ ] Today `SafeCall.kt` + 4 `*TableSync.kt` catch
      `android.database.sqlite.SQLiteException` / `SQLiteConstraintException`.
      These types don't exist on iOS native.
- [ ] Introduce an `expect fun mapSqlException(t: Throwable): DomainException?`
      (or a `expect` boolean `isConstraintViolation(t)`), with:
      - `androidMain`: matches the Android SQLite exception types (current behavior).
      - `iosMain`: matches SQLDelight's native exception surface.
- [ ] Refactor `SafeCall` + TableSync to funnel through the expect helper
      instead of importing `android.database.sqlite.*` directly.
- **Gate:** Android green + `SafeCall`/sync tests green + iOS compile green.

**2e — Ktor engine per source set (D5)**
- [ ] `androidMain`: `ktor-client-okhttp` (current).
- [ ] `iosMain`: `ktor-client-darwin`.
- [ ] Supabase client creation (`SupabaseModule` in app) takes the engine via
      Koin per platform, or Supabase-kt picks the default engine per target.
- **Gate:** Android green + iOS compile green.

---

### Phase 3 — `shared-ui` (Compose Multiplatform) — HIGH RISK / LARGEST
**Scope:** extract Compose UI + ViewModels + MVI + Koin modules from `:app` into
a shared module. 196 files don't move in one PR — slice by feature.
- [ ] Create `shared-ui` module: `kotlin.multiplatform` + `androidLibrary{}` +
      ios targets + `composeMultiplatform` + `composeCompiler`. Framework
      `baseName = "Shared"`, `isStatic = true` (as in the wizard).
- [ ] Swap Jetpack Compose artifacts → Compose Multiplatform
      (`org.jetbrains.compose.*` / `compose.material3`, `compose.foundation`, etc.).
- [ ] Move MVI base (`core/mvi/`) → `shared-ui/commonMain` first (foundation).
- [ ] Move features in slices (one PR per feature group), each gated:
      transactions → categories → recurring → home → auth/sync UI.
      Use `lifecycle-viewmodel-compose` multiplatform for ViewModels.
- [ ] Move the 9 Koin modules that are platform-agnostic into `shared-ui`;
      platform-specific bindings (driver context, Firebase) stay in `androidApp`.
- [ ] **Compose Resources**: migrate `res/` strings/drawables used by shared
      screens into `commonMain/composeResources/`. Spanish UI strings stay
      Spanish. ⚠️ Google Fonts (`androidx.ui.text.google.fonts`) is Android-only
      — needs a Compose Multiplatform font strategy (bundle the font, or
      `expect/actual` the FontFamily).
- **Gate per slice:** `./gradlew assembleDevDebug` green; the app still runs
  with the moved screens served from `shared-ui`.

---

### Phase 4 — split `:app` → `androidApp` — MEDIUM RISK
**Scope:** gut the old `:app` down to a thin Android entry point.
- [ ] Rename/restructure `:app` → `androidApp` (`android.application`).
- [ ] Keep ONLY: `MainActivity`, `EmmApp` (Application), `google-services` +
      Firebase BOM + Crashlytics, Google Sign-In (Credential Manager /
      `google-identity-googleid`), `AndroidManifest`, Android-specific Koin
      bootstrap (driver `Context`, Firebase), launcher/theme resources.
- [ ] `androidApp` depends on `shared-ui` (which transitively brings `data`,
      `domain`). Use typesafe accessors `projects.sharedUi` (enable
      `TYPESAFE_PROJECT_ACCESSORS` like the wizard).
- [ ] Update `settings.gradle.kts` module names.
- **Gate:** full app builds and runs from `androidApp`. This is the proof the
  Android product is intact post-extraction.

---

### Phase 5 — `iosApp` first run — iOS APPEARS — MEDIUM RISK
**Scope:** get Compose UI rendering on an iOS simulator.
- [ ] Wire the wizard's `iosApp/` Xcode project to link the `Shared` framework
      produced by `shared-ui`.
- [ ] Implement iOS actuals confirmed working: `NativeSqliteDriver` (2b),
      Darwin engine (2e), exception mapping (2d), Clock (2c).
- [ ] `MainViewController()` entry point in `shared-ui/iosMain` returning the
      root Compose screen; `ContentView.swift` hosts it (copy wizard pattern).
- [ ] Koin init for iOS (no Android Context — provide iOS driver/paths).
- **Gate:** app launches in iOS simulator, local-first flows (add/list
  transactions, categories) work **with no account, no network** — the
  local-first core must work on iOS before touching sync/auth.

---

### Phase 6 — iOS parity: auth, sync, telemetry — MEDIUM/HIGH RISK
**Scope:** close the platform-specific gaps. Each is an independent decision.
- [ ] **Supabase sync on iOS** — should mostly work via supabase-kt KMP +
      Darwin engine. Verify push/pull/cursor against the same backend.
- [ ] **Google Sign-In on iOS** — `google-identity-googleid` (Credential
      Manager) is **Android-only**. iOS needs the GoogleSignIn iOS SDK behind an
      `expect/actual` auth bridge, OR email/password-only on iOS for v1.
      → **OPEN DECISION** (see §5).
- [ ] **Firebase Crashlytics / Analytics on iOS** — separate Firebase iOS SDK +
      `GoogleService-Info.plist`. Or defer telemetry on iOS for v1.
      → **OPEN DECISION** (see §5).
- [ ] **Connectivity-regained sync trigger** — the open tech-debt item
      (`SyncOrchestrator`, see root `CLAUDE.md`) uses `ConnectivityManager`
      (Android). For iOS it needs `NWPathMonitor` behind `expect/actual`. Track
      with the existing debt note.
- **Gate:** iOS feature set matches the agreed v1 scope.

---

## 4. Cross-cutting concerns (don't let these escape)

- **Tests / MockK:** MockK is JVM-only. Strategy: keep MockK-based unit tests in
  `androidHostTest`/`androidUnitTest`; for behavior that must be verified on iOS,
  write `commonTest` with hand-rolled fakes + `kotlin-test`. Don't try to force
  MockK into `commonTest`.
- **minSdk:** `:data`=26, `:app`=28. KMP fine. iOS deployment target set in
  Xcode (`Config.xcconfig`); pick iOS 15+ to match Compose Multiplatform support.
- **Sync cursor format (ADR-002):** any time/serialization change in Phase 2c
  must preserve the exact cursor string format so already-synced devices keep
  working. Mandatory round-trip test.
- **Supabase keys / BuildConfig:** `GOOGLE_WEB_CLIENT_ID` and Supabase URL/anon
  key come from `supabaseProperties` via `buildConfigField` (Android). iOS needs
  these injected too (xcconfig / generated Kotlin constant). Plan a shared config
  source so keys aren't duplicated.
- **Compose Resources:** Jetpack `R`/`stringResource` → Compose Multiplatform
  `Res.string.*`. Spanish UI strings stay Spanish (project convention).
- **Koin:** 4.2 is KMP. Platform modules (`DbModule`, `SupabaseModule`) split
  into common + platform actuals; feature modules go fully common.
- **Detekt / CI:** the build verb changes (`:app:` → `:androidApp:`,
  `:domain:test` may become `:domain:testDebugUnitTest` or `iosTest`). Update
  CI, detekt config, and `CLAUDE.md` build commands at the end of each phase.
- **Firebase google-services plugin:** must NOT be applied to KMP library
  modules — only `androidApp`. Applying it to `shared-ui`/`data` breaks the
  iOS/native compilation.

---

## 5. Open decisions (need @emm input before the relevant phase)

1. **iOS auth (Phase 6):** Google Sign-In native on iOS (GoogleSignIn SDK +
   bridge) vs. email/password-only for iOS v1? *Recommendation: email/password
   for first iOS release, add Google later — smaller surface, faster to ship.*
2. **iOS telemetry (Phase 6):** Firebase Crashlytics/Analytics on iOS now, or
   defer? *Recommendation: defer for the iOS alpha; add before public release.*
3. **Compose vs native SwiftUI (architectural):** confirmed **shared Compose UI**
   for now. Revisit only if iOS UX feels non-native enough to justify the
   `sharedLogic`+`sharedUI` split.
4. **Module naming:** `shared-ui` vs `app-shared` vs `presentation` — pick a name
   before Phase 3.

---

## 6. Sequencing summary (the green path)

```
Phase 0  scaffolding        ── Android green (no-op)
Phase 1  domain → KMP       ── Android green + iOS compiles
Phase 2  data → KMP (2a-2e) ── Android green at each sub-PR + iOS compiles
Phase 3  shared-ui (Compose)── Android green per feature slice
Phase 4  split → androidApp ── Android app intact from new entry point
Phase 5  iosApp first run   ── iOS local-first works in simulator
Phase 6  iOS parity         ── sync/auth/telemetry per agreed scope
```

**Rule restated:** if `./gradlew assembleDevDebug` is red, the phase isn't done.

---

## 7. References
- JetBrains new KMP default structure (May 2026):
  https://blog.jetbrains.com/kotlin/2026/05/new-kmp-default-structure/
- Create your Compose Multiplatform app:
  https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-create-first-app.html
- Compose Multiplatform 1.8.0 — iOS stable:
  https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/
- Wizard reference project: `~/Downloads/KotlinProject.zip`
  (unzipped audit at `/tmp/kp_extract/KotlinProject`)
- Project ADRs: `docs/adr/001` (local-first), `docs/adr/002` (pull cursor)
- Sync plan: `docs/sync/PLAN.md`
