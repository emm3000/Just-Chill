# E07-02 — Clear the `:presentation` baseline

**Epic:** [E07 — baseline burn-down](../epics/E07-baseline-burndown.md)

## Done when

- [ ] `baseline-presentation-main.xml`'s 14 entries are gone, or each survivor is named in the
      closing commit with the reason it survived
- [ ] `RecurringMovementUi.kt`'s `amount!!` is resolved by deciding what a null amount means for the
      row, not by moving the crash somewhere quieter
- [ ] `NumberFormatEs.kt`'s three `MagicNumber` (`0.5`, `100.0`, `3`) become named constants that say
      what they are, not `THREE`
- [ ] `InjectDispatcher:CoreModule.kt:Default` is judged, not reflexively fixed: if that module is
      the seam where the dispatcher is injected from, the entry stays with its reason stated
- [ ] every formatter's output is byte-identical afterwards — `SpanishDateFormat`, `CentsFormatter`,
      `NumberFormatEs` and `SpanishSearch` produce user-facing Spanish, addressed as `tú`
- [ ] no threshold in `config/detekt/detekt.yml` is relaxed, no new `@Suppress`, no baseline
      hand-edited and no entry added
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`:presentation` commonMain is the surface exported to iOS as `JustChillKit`, so this unit gets a
mandatory review (`docs/WORKFLOW.md`) and must not introduce a `java.*`/`android.*` import — the
gate's iOS compile is what proves it.

Ten of the fourteen are mechanical: four `FunctionSignature`, one `ClassSignature`, one
`ArgumentListWrapping` (collapse to one line — see `CLAUDE.md`), and three `UseOrEmpty` that want
`orEmpty()`. The `amount!!` is the only one carrying behavior risk; `docs/CODE_QUALITY.md`'s date
rule applies if `SpanishDateFormat` is touched at all — injected `Clock` **and** `TimeZone`, no
defaults.
