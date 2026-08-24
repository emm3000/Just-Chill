# E06-04 — Swap the Inicio tab for Reporte

**Epic:** [E06 — Navigation shell](../epics/E06-navigation-shell.md)
**Depends on:** E06-02, E06-03

## Done when

- [ ] `ReportRoute` is a `BottomBarRoute` labelled `"Reporte"`; the bar reads Ver, Reporte, the add button, Cuentas, Perfil
- [ ] `ReportTopBar` no longer takes an `onBack` and renders no back button; the share action survives
- [ ] every `Home` symbol is gone — route, screen and previews, nav entries, ViewModel, UiState,
      Intent, Effect, the Koin module and its app-graph inclusion, and `GetHomeDataUseCase` with its
      test — `rg Home` under `presentation/src`, `ui-android/src`, `androidApp/src` finds nothing for it
- [ ] `AppNavigatorTest` picks a different non-start tab for its `rootTab`, keeping the property that
      the navigator uses the tab it is handed rather than the platform default
- [ ] `RouteSerializationTest` round-trips `ReportRoute` and no longer names `HomeRoute`
- [ ] the Koin graph test's ViewModel registry no longer lists `HomeViewModel`, and the DI test passes
- [ ] the detekt baseline entries for `HomeScreen.kt` are removed rather than left pointing at a deleted file
- [ ] E05-11's `Done when` no longer cites Home's `Saldo total` — repoint it at a surface that still exists, in this same commit
- [ ] `./gradlew qualityGate` and `./gradlew assembleDevDebug` both pass, and the app launches on the Ver tab with Reporte reachable in one tap

## Context

This is atomic on purpose — splitting it leaves a commit where Home is either a dead tab or an
unreachable route. E06-02 and E06-03 must land first, or deleting Home strands Préstamos and the
pending recurrents. ADR 003's item 6 about `startTab` is settled here; ADR 003 itself stays unedited.
