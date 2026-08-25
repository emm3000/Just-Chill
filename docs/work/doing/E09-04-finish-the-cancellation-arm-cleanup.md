# E09-04 — Finish the cancellation-arm cleanup

**Epic:** [E09 — Faster transaction entry](../epics/E09-faster-transaction-entry.md)

## Done when

- [ ] `EditTransactionViewModel.loadFrequent` no longer wraps a suspend call in `runCatching`, and
      `rg 'runCatching' presentation/src/commonMain` returns no hit inside a ViewModel.
- [ ] The `Flow.catch` paragraph in `docs/CODE_QUALITY.md` names both cases `catchImpl` rethrows on —
      the collecting job's own cancellation cause **and** anything originating downstream — instead of
      claiming the lambda sees every other `CancellationException`.
- [ ] The cancellation test in `AddTransactionViewModelTest` carries a positive control, so it fails
      naming what did not happen rather than passing on an unreached suspension.
- [ ] The comment above `loadOrNull` states the value the stale call actually writes at the
      `loadFrequent` sites (`.orEmpty()` makes it an empty list, not null).
- [ ] `./gradlew qualityGate` green with the touched host-test tasks forced via `--rerun`.

## Context

All four descend from the Judgment Day fix round on E09 (`f542c8f3`..`1fea4310`) and are the leftover
`info` rows, none blocking. The `EditTransactionViewModel` one is the last `runCatching` around a
suspend call in the app and it contradicts the paragraph that round added to `CODE_QUALITY.md`.
