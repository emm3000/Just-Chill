# E07-06 — Name `:androidApp`'s detekt resolution errors

**Epic:** [E07 — baseline burndown](../epics/E07-baseline-burndown.md)

## Done when

- [ ] the symbols `:androidApp:detektDevDebug` fails to resolve are named, and the source files
      referencing them
- [ ] every detekt rule that silently stopped firing on `:androidApp` because of them is listed —
      that list, not the error count, is the point of this ticket
- [ ] the shared root cause with [E01-37](E01-37-restore-detekt-type-resolution-in-data.md) is
      confirmed or ruled out; if it is the same exclude-drops-resolution-scope mechanism, the two
      tickets close together
- [ ] either the errors are gone, or the closing commit records why detekt `2.0.0-alpha.6` cannot fix
      it and `docs/CODE_QUALITY.md` names the limitation beside the `:data` one
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

`:androidApp:detektDevDebug` prints `There were N compiler errors found during analysis. This affects
accuracy of reporting.` and still exits 0. The count is non-deterministic — 12 to 25 across runs of
the same sources — which is itself worth explaining.

Confirmed pre-existing during E09-02, by running the task in a worktree at `4ea73a6a`, before that
ticket touched any code. Same symptom as E01-37 in a different module; that ticket's suspicion is
that excluding generated sources drops them from detekt's *resolution* scope as well as its
*analysis* scope.
