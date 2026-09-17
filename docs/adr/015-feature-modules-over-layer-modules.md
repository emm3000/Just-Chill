---
status: accepted
date: 2026-09-17
---
# Feature modules over layer modules

Every feature is spread across `:domain`, `:data`, `:presentation` and
`:ui-android`, and the convention plugins cover only detekt, the quality gate
and build info. Features reach into each other freely (shared sheets, loan
helpers on the accounts screen, recurring pendings on the transaction list),
and nothing but review stops them. Gema solves the same problem with core
modules, one module per feature and convention plugins, and we want one shape
across both repos with the boundaries enforced by the gate.

## Decision

- Modules: `:androidApp`, `:core:domain` (pure JVM), `:core:database`
  (SQLDelight), `:core:backup` (Supabase, Ktor, the snapshot DTOs), `:core:ui`
  (theme, atoms, `MviViewModel`, formatters and every UI piece more than one
  feature uses), and `:feature:{transaction, account, category, recurring,
  report, loan, profile, auth, onboarding}`. The transaction list lives in
  `:feature:transaction`; the backup UI lives in `:feature:profile`.
- Edges: only `:androidApp` depends on a feature; a feature depends on
  `:core:domain` and `:core:ui` only; a core module depends on `:core:domain`
  only. `checkModuleBoundaries` enforces them inside `justchill.quality.gate`.
- A feature module holds its ViewModels and its screens together. The
  compose-free rule survives as a gate check that forbids `androidx.compose` in
  `*ViewModel` and `*UiState` files, not as a module boundary.
- Navigation stays on Navigation 3. Each feature owns its `@Serializable`
  routes and its entries and receives cross-feature navigation as lambdas;
  `:androidApp` wires them and owns `RouteSerializationTest`.
- Koin is assembled in `:androidApp`: the database, `Clock`, `TimeZone`,
  repositories and use cases are bound there, and core modules do not depend
  on Koin. Each feature exposes a module with its ViewModels only.
- Convention plugins live in `build-logic/convention/`:
  `justchill.android.{application,library,compose,feature,release}`,
  `justchill.jvm.library`, `justchill.sqldelight`, plus the existing detekt,
  quality gate and build info plugins. `minSdk` is 28 everywhere.
- Packages become `com.emm.justchill.<core|feature>.<name>`; `hh` goes. The
  SQLDelight database becomes `JustChillDatabase` in
  `com.emm.justchill.core.database`.
- The migration runs in phases, one pull request per ticket, with the gate
  green after each: plugins, core modules, cross-feature untangling, one
  feature at a time, then the removal of `:presentation` and `:ui-android`.

## Invariants

The device keeps real data, so these names never change: the database file
`com.emm.data.db`, the preference files `justchill_prefs` and
`justchill_auth` with their keys, `com.emm.justchill.MainActivity` (pinned
shortcuts store it) and the `applicationId`. The schema does not change; the
migration suite runs on `medium_phone` before and after `:data` moves.

## Considered options

- Two modules per feature (`presentation` and `ui`) keeps compose-free as a
  compile-time boundary but doubles the module count for a rule a gate check
  already holds.
- Routes in a shared core module would let features navigate directly, at the
  price of every feature knowing every other feature's routes.
- Navigation 2 with string routes, as Gema has, would throw away the typed
  routes that already work.
- Core modules exposing their own Koin modules would spread the composition
  root and put Koin on the classpath of the data layer.
