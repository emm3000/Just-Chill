---
paths:
  - "ui-android/src/main/kotlin/com/emm/justchill/hh/shared/**"
  - "ui-android/src/main/kotlin/com/emm/justchill/hh/**/*Entries.kt"
  - "ui-android/src/test/kotlin/com/emm/justchill/hh/shared/RouteSerializationTest.kt"
---

# Routes and the back stack

- Every route the host can push is `@Serializable`, fields included. `rememberNavBackStack` stores
  each entry by class name and re-resolves it through `Class.forName(name).kotlin.serializer()`,
  so an unserializable route crashes on process-death restore and nowhere else: the compiler and
  the gate see nothing. `RouteSerializationTest` reflects over sealed `AppRoute` and round-trips
  every route through that exact serializer pair; a new route, and a route promoted to
  `BottomBarRoute`, shows up there.
- One door is no door. A feature reachable through exactly one entry point is unreachable the
  moment that entry moves or is gated: `LoansCard` behind `hasLoans` hid `LoansRoute` (`15b8783a`),
  and `HomeEntries` was the sole push of `ReportRoute`. Give a destination a door that does not
  depend on a screen the author does not open, and gate the content of an entry point, never its
  existence. Before deleting a row that pushes a route, `rg` the route and confirm a second door
  exists or the route is dead. No UI test harness catches any of this.
- Moving to a route that may already be on the stack uses `AppNavigator.pushToTop`, never `push`:
  `push` guards with `backStack.contains(route)`, a duplicate guard that silently does nothing once
  the target is buried. `pushToTop` matches by runtime class, pops what sits above, and reveals an
  equal route or replaces one carrying stale arguments.
- `NavEntry.content` closures are cached until the back stack changes. Host state an entry reads
  arrives as a `() -> T` accessor, never by value; the result channels in `AppNavHost.kt`
  (`pendingCategory`, `pendingImportJson`) are the pattern.
