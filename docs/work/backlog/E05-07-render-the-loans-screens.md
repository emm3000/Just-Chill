# E05-07 — Render the loans screens

**Epic:** [E05 — Loans](../epics/E05-loans.md)
**Blocked by:** E05-06

## Done when

- [ ] `:ui-android` gains `LoansScreen` (the people list, with an empty state) and
  `PersonLoansScreen`
- [ ] both routes are declared `@Serializable` in `HhRoutes.kt`
- [ ] `RouteSerializationTest` round-trips both new routes
- [ ] `AppNavigator.kt` gains the nav entries reaching both screens

## Context

A route missing `@Serializable` only fails on process-death restore — invisible to the compiler,
which is why `RouteSerializationTest` is the check, not a manual smoke test.
