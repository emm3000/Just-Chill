# E01-39 — Decide `ignoreDefaultParameters` for the Compose module

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Blocks:** [E07-04](E07-04-shorten-the-long-composable-signatures.md)

## Done when

- [ ] the question is answered in `docs/CODE_QUALITY.md`, not just in a commit: does a parameter
      carrying a default count toward `LongParameterList` in a Compose module
- [ ] if the answer is no, `ignoreDefaultParameters` flips to `true` and every baseline entry it
      frees is regenerated away — measured at 12 of 19 in `:ui-android`; re-measure, do not trust
      that number
- [ ] if the answer is yes, `docs/CODE_QUALITY.md` says why, so the next reader stops re-asking
- [ ] whichever way it goes, the flag is set once for every module — `:ui-android` does not get its
      own detekt config
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass

## Context

This is the one place E07's "never relax the rule" does not apply, because it is not a burn-down
ticket: it asks whether the rule is configured right in the first place. That is why it lives here
and not under E07.

`allowedFunctionParameters: 5` with `ignoreDefaultParameters: false` counts `modifier: Modifier =
Modifier`, `enabled: Boolean = true` and every other defaulted parameter. Compose's own components
routinely exceed 5 that way, so the current setting reports the framework's idiom as a smell. detekt
ships the flag precisely for this case.

The counter-argument is real and should be weighed, not waved off: a default does not make a
parameter free to read at a call site.
