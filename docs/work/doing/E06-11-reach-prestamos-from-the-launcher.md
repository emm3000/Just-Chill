# E06-11 — Reach Préstamos from the launcher

**Epic:** [E06 — navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] long-pressing the app icon offers "Préstamos", and dragging that item onto the home screen
      leaves a standalone icon that opens the same place
- [ ] the shortcut seeds the back stack as `[startTab, LoansRoute]`, so Back returns into the app
      rather than leaving it
- [ ] tapping the shortcut while the app is already open still navigates — `MainActivity` handles
      `onNewIntent`, not only `onCreate`
- [ ] a first launch still lands on `ManifestoRoute`: the onboarding guard outranks the shortcut
- [ ] the shortcut works in the dev flavor, whose own `res/xml/shortcuts.xml` shadows `main`'s
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Préstamos has exactly one door — `AccountEntries.kt` pushes `LoansRoute` from the Cuentas screen —
which is the first constraint in this epic, already bitten twice.

`MainActivity` reads no Intent, and `AppNavHost` computes `startRoute` internally with no parameter,
so an initial route has to be plumbed through both. `LoansRoute` is already `@Serializable` and
appears in `RouteSerializationTest`.
