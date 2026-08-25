# E01-38 — Settle `DispatchersProvider`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `KoinIos.kt`'s comment about `DispatchersProvider`'s consumers is corrected or deleted — it
      names the dev-flavor `ExperiencesLocalDataSource` as the only one, and `EmmApp`'s
      `sweepLegacySession` also resolves it, in `androidApp/src/main`, both flavors
- [ ] `DispatchersProvider` either earns its place in `:presentation` commonMain or leaves it: both
      consumers live in `:androidApp`, so today it rides into the `JustChillKit` framework unbound
- [ ] whichever way it goes, `./gradlew :presentation:linkDebugFrameworkIosSimulatorArm64` still
      links — moving a type out of the exported surface changes what SKIE generates
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Surfaced by the E07-02 review, both pre-existing and both about the same symbol.

Leaving it *unbound* on iOS is correct — binding it there would be dead wiring for a type nothing on
iOS resolves. Having it in commonMain at all is the question: `docs/CODE_QUALITY.md`'s YAGNI rule
asks whether an abstraction answers a real need, and an interface whose every consumer is
Android-only answers an Android-only need.

The cheap resolution is a move to `:androidApp`; deletion is on the table only if both consumers can
take a `CoroutineDispatcher` directly. Do not keep it in commonMain and bind it on iOS just to make
the shape symmetric — that trades a YAGNI abstraction for a YAGNI abstraction plus dead wiring.
