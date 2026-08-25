# E06-12 — Pin the shortcut's decisions

**Epic:** [E06 — navigation shell](../epics/E06-navigation-shell.md)

## Done when

- [ ] `shortcutRouteToPush(action, firstLaunchSeen, currentTop): NavKey?` is a pure function in
      `:ui-android`, and `AppNavHost`'s `LaunchedEffect` body is its call plus the push, nothing else
- [ ] host tests pin all three guards it now owns: an unknown or null action yields no route; a known
      action yields none while `firstLaunchSeen` is false; a route already on top yields none
- [ ] a host test fails when the `android:action` in either `androidApp/src/main/res/xml/shortcuts.xml`
      or `androidApp/src/dev/res/xml/shortcuts.xml` stops matching `ACTION_OPEN_LOANS`
- [ ] the E06 constraint claiming nothing pins that string is deleted — a test replaced the prose
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

E06-11 verified the onboarding guard, the dedupe and the back-stack seeding by hand on an emulator
and recorded the risk as prose. `docs/work/README.md`'s own gate says a fact with a fix is a ticket,
not a constraint, so that entry was misfiled.

There is no Compose UI test infrastructure here — `androidDeviceTest` exists only in `:data` — so the
logic has to leave the effect to be testable. `AppNavigatorTest` is the precedent for nav logic as a
pure host test.
