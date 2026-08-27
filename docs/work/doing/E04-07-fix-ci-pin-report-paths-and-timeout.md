# E04-07 — Fix the setup-gradle pin, buildDev's stale report paths and its timeout

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] `gh api repos/gradle/actions/commits/<new-sha>` returns 200 for the SHA now in
      `.github/actions/setup-android/action.yml`'s `Setup Gradle` step, and the trailing comment
      names the real `vN.N.N` tag that resolves to it
- [ ] the comment above that step states only what `gh api .../git/tags/<old-sha>` actually showed
- [ ] `rg 'testAndroidHostTest' .github/workflows/buildDev.yml` returns nothing; each of the four
      corrected paths matches a real `build/reports/tests/<task>/` directory produced by the task
      each module names in its own `build.gradle.kts`
- [ ] `.github/workflows/buildDev.yml`'s `quality-gate` job has `timeout-minutes: 45`
- [ ] `actionlint` exits 0 over every file in `.github/workflows/` and `.github/actions/`

## Context

Pre-push audit ahead of the first-ever CI run on 137+ local commits. `setup-gradle` was pinned to an
annotated tag object (`gh api .../commits/<sha>` → 422), not a commit — its own comment falsely
claimed otherwise. `buildDev.yml`'s test-report paths are KMP-era (`testAndroidHostTest`, retired by
ADR 011); only `:androidApp`'s path was ever correct. `if-no-files-found: error` added to that step
so a future path drift fails loud instead of the default `warn`. Full evidence, `gh api` transcripts
and third-party pin audit results: this session's report.
