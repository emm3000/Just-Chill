# E01-37 — Restore detekt type resolution in `:data`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] every detekt rule that silently stopped firing on `:data` while resolution was broken is
      listed; that list, not the error count, is what this ticket is about
- [ ] `:data:detektMainAndroid --rerun-tasks` still reports no compiler errors, and each listed rule
      is shown either firing or provably clean
- [ ] `docs/CODE_QUALITY.md` records what a compiler-error line under detekt costs, now that the
      `iosMain` example it used is gone
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

**The nine errors are gone as of E11-02** (`b93f8090`): they were the three `expect` declarations in
`:data`, not the excluded SQLDelight output this ticket first blamed. `:data:detektMainAndroid
--rerun-tasks` now analyses clean. What survives is box 2 — nobody has ever listed which rules were
silently off — and box 4.

Excluding generated sources is still right: SQLDelight output is not ours to lint. Whether that
exclude also costs resolution scope is now an open question rather than a measured fact.
