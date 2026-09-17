---
paths:
  - ".github/**"
  - "androidApp/build.gradle.kts"
  - ".claude/commands/release.md"
  - "scripts/git-hooks/**"
---

# CI, release and the gate

- `./gradlew qualityGate` is the gate, invoked by the pre-push hook and the three build workflows (`buildDev`, `uploadApk`, `uploadRelease`; `probeStorageRls` runs no Gradle). The plugin `build-logic/.../QualityGateConventionPlugin.kt` matches `detektMain`/`detektTest`, `:data`'s instrumented compile and `verifySqlDelightMigration`, and names `:build-logic:test` explicitly (an included build is unreachable by task-name matching). The test suites and dev lint are NOT in the plugin: each module names its own in its `build.gradle.kts`, and a module that stops naming one leaves the gate silently. Never gate on plain `./gradlew detekt`: it covers strictly less.
- Third-party actions are pinned to a commit SHA on purpose: they hold the signing and Play/Firebase credentials, and a floating `@v1` can be repointed upstream. Do not tidy them into tags; dependabot proposes bumps. GitHub's own `actions/*` stay on tags.
- `versionName` is `git describe --tags --abbrev=0 --match "v[0-9]*"`. The filter is load-bearing (the repo carries non-release tags; builds once shipped `versionName = "pre-kmp"`), and `--abbrev=0` means it is always the bare tag, so `versionCode` is what names a build. Same filter in `/release`.
- A tag push does not ship. `uploadRelease.yml` uploads the AAB to the alpha track as a draft; publishing is manual in Play Console. A green workflow reached no one.
- `run:` blocks take secrets through `env:`, never `${{ }}` spliced into the script text. Validate workflow edits with `actionlint`.
