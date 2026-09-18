---
status: done
date: 2026-09-18
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

## Waves

Waves 1 to 4 are serial because each rewrites imports or build files across
the whole repo; parallelism lives in waves 5, 7 and 8.

1. `build-logic/convention/` and the plugins applied to the five modules.
2. `:domain` becomes `:core:domain`.
3. `:data` splits into `:core:database` and `:core:backup`; the migration
   suite runs on `medium_phone` before and after.
4. `:core:ui` takes the MVI base, theme, atoms, formatters.
5. Untangling, in parallel: shared sheets and components, recurring pendings,
   loan balances, `hh/shared` off `profile`.
6. Scaffold: the nine empty `:feature:*` modules, their includes and
   `:androidApp` dependencies, and one wiring file per feature in
   `:androidApp` (Koin module, entries, navigation lambdas).
7. and 8. Feature extraction, four or five per wave; each ticket touches its
   own module, deletes its own packages and edits its own wiring file only.
9. Delete `:presentation` and `:ui-android`; update the module docs and rules.

## Invariants

The device keeps real data, so these names never change: the database file
`com.emm.data.db`, the preference files `justchill_prefs` and
`justchill_auth` with their keys, `com.emm.justchill.MainActivity` (pinned
shortcuts store it) and the `applicationId`. The schema does not change; the
migration suite runs on `medium_phone` before and after `:data` moves.

## Amendments (2026-09-18)

- A sealed route hierarchy cannot span modules: routes unsealed, `AppRoute` / `BottomBarRoute` / `CaptureRoute` moved to `:core:ui`'s `navigation/` alongside `AppNavigator` and `NavHostBindings`.
- `:core:testing` added (JVM, `MainDispatcherRule` + `FakeTodayFlow`, depends on `:core:domain` only); a feature depends on `:core:domain` and `:core:ui`, plus `:core:testing` on the test edge only.
- `RouteSerializationTest` concatenates one route registry per feature instead of walking `sealedSubclasses`.
- Waves 6 and 7 merged: `:feature:{account, category, loan, report}` populated.
- Wave 9 done: `:presentation` and `:ui-android` deleted. `:androidApp` took the backup cycle, the lifecycle edges, `AppPreferences` and the four cross-cutting Koin modules as `core/di/`, and `rememberPlatformHostActions` as `shell/`; every other file in them was dead and went. `ModuleRole` lost `LAYER` and gained `ROOT`, so an unrecognised module path now fails `checkModuleBoundaries` instead of falling through.

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
