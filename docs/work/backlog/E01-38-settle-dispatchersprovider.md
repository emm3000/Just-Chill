# E01-38 — Settle `DispatchersProvider`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `TestPlatformModule.kt`'s comment about `DispatchersProvider`'s consumers is corrected — it
      names the dev-flavor `experiencesModule` as the only one, and `EmmApp`'s `sweepLegacySession`
      also resolves it, in `androidApp/src/main`
- [ ] `DispatchersProvider` either earns its place in `:presentation` commonMain or leaves it: both
      consumers live in `:androidApp`, so today nothing outside `:androidApp` needs it there
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

Surfaced by the E07-02 review, both pre-existing and both about the same symbol.

`docs/CODE_QUALITY.md`'s YAGNI rule asks whether an abstraction answers a real need, and an
interface whose every consumer is Android-only answers an Android-only need.

The cheap resolution is a move to `:androidApp`; deletion is on the table only if both consumers can
take a `CoroutineDispatcher` directly.
