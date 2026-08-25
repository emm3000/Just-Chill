# E01-37 — Restore detekt type resolution in `:data`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] the nine errors `:data:detektMainAndroid` reports are named — which symbols fail to resolve and
      which source files reference them
- [ ] every detekt rule that silently stopped firing on `:data` because of them is listed; that list,
      not the error count, is what this ticket is about
- [ ] either the errors are gone and `:data:detektMainAndroid` analyses with full type resolution, or
      the closing commit records that detekt `2.0.0-alpha.6` cannot separate the two concerns and
      this ticket dies with that finding
- [ ] `docs/CODE_QUALITY.md` names the limitation beside the iosMain one it already documents
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`:data:detektMainAndroid` prints `There were 9 compiler errors found during analysis. This affects
accuracy of reporting.` and still exits 0. Pre-existing since `c94e2901`, surfaced by the E07-01
review.

Excluding generated sources is right — SQLDelight output is not ours to lint — but the exclude drops
them from detekt's *resolution* scope as well as its *analysis* scope, so hand-written `:data` code
referencing `TransactionsQueries` and friends is analysed against unresolved symbols, and rules
needing type resolution stop firing silently, with no failing task.

Worth checking first: detekt's task exposes `classpath` separately from `source`. Putting the
generated roots on the former without adding them to the latter would resolve the types without
linting them — if the alpha honours that split.
