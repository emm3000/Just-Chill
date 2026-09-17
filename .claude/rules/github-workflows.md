---
paths:
  - ".github/**"
  - "androidApp/build.gradle.kts"
  - ".claude/commands/release.md"
  - "scripts/git-hooks/**"
---

# CI, release and the gate

- `./gradlew qualityGate` is the gate, invoked by the pre-push hook and the three build workflows (`buildDev`, `uploadApk`, `uploadRelease`; `probeStorageRls` runs no Gradle). The plugin `build-logic/.../QualityGateConventionPlugin.kt` matches `detektMain`/`detektTest`, `:data`'s instrumented compile and `verifySqlDelightMigration`, and names `:build-logic:test` explicitly (an included build is unreachable by task-name matching). The test suites and dev lint (`:androidApp:lintDevDebug`) are NOT in the plugin: each module names its own in its `build.gradle.kts`, and a module that stops naming one leaves the gate silently. Never gate on plain `./gradlew detekt`: it covers strictly less.
- Third-party actions are pinned to a commit SHA on purpose: they hold the signing and Play/Firebase credentials, and a floating `@v1` can be repointed upstream. Do not tidy them into tags; dependabot proposes bumps. GitHub's own `actions/*` stay on tags.
- `versionName` is `git describe --tags --abbrev=0 --match "v[0-9]*"`. The filter is load-bearing (the repo carries non-release tags; builds once shipped `versionName = "pre-kmp"`), and `--abbrev=0` means it is always the bare tag, so `versionCode` is what names a build. Same filter in `/release`.
- A tag push does not ship. `uploadRelease.yml` uploads the AAB to the alpha track as a draft; publishing is manual in Play Console. A green workflow reached no one.
- `run:` blocks take secrets through `env:`, never `${{ }}` spliced into the script text. Validate workflow edits with `actionlint`.
- `trunk` protection has `enforce_admins: false` on purpose: direct pushes to `trunk` skip CI, so the pre-push hook running `qualityGate` is the only net. Turning it on means every change needs a branch and PR. Change protection with a `PUT` of the whole object, never a PATCH. Check it: `gh api repos/emm3000/Just-Chill/branches/trunk/protection --jq '{checks: .required_status_checks.contexts, admins: .enforce_admins.enabled, linear: .required_linear_history.enabled, force: .allow_force_pushes.enabled}'`.
- The pre-push hook (`scripts/git-hooks/pre-push`, runs `qualityGate` only) is off after clone; opt in once with `git config --local core.hooksPath scripts/git-hooks`. Change the gate in the plugin, never in the hook.
