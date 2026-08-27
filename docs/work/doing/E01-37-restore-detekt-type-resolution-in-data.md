# E01-37 — Restore detekt type resolution in `:data`

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `:data:detektDebug --rerun-tasks` reports no `compiler errors found during analysis` line.
      It reports **472** today; `:presentation` and `:ui-android` report none on the same run, so
      the cause is specific to this module.
- [ ] The seven `@file:Suppress("RedundantSuspendModifier")` in `:data`'s `*LocalDataSource.kt` are
      deleted and detekt stays green. They cover 47 `suspend fun` between them and exist only
      because the broken analysis cannot see that `withContext` suspends.
- [ ] A canary proves type resolution works, rather than that the error line merely stopped printing:
      introduce one violation of a rule that fires ONLY under type resolution, show detekt catching
      it, revert it. Name the rule in the commit message.
- [ ] `config/detekt/baseline-data-debug.xml` and `-release.xml` still carry exactly their one
      `InjectDispatcher` entry — a fix that needs new baseline entries is not a fix.
- [ ] `./gradlew qualityGate --rerun-tasks` and `./gradlew assembleDevDebug` both pass.

## Context

`DetektConventionPlugin.excludeGeneratedSources()` drops SQLDelight output from detekt's *resolution*
scope as well as its *analysis* scope, so code referencing `TransactionsQueries` is analysed against
unresolved symbols. The task exposes `classpath` separately from `source`: generated roots on the
former only would resolve without linting, if the alpha honours the split. E11-05's conversion raised
the count from nine to 472; measured again after E11-07, still 472.
