# E13 — Dead vocabulary

**Follows:** ADR 011 (Android-only, KMP build and iOS target dropped). The build converted;
comments and doc prose lagged.

## Constraint

No comment, doc line or build-file note may name a source set, plugin, target or module layout
that does not exist in the repo. `commonMain`, `androidMain`, `iosMain`, `commonTest`,
`androidHostTest` and `androidDeviceTest` are gone — `src/main`, `src/test`, and `:data`'s
`src/androidTest` are what exist. A comment describing a dead layout is a false statement about
the code, not a style nit, and the next writer will act on the false statement.

**The exception is the intentionally historical mention**, load-bearing for a future reader — e.g.
why `--match "v[0-9]*"` is not optional (`versionName` once shipped `"pre-kmp"`), or why
`multiplatform-settings` stays (`gradle/libs.versions.toml`) even though it is KMP-branded. Test
it with one question: **if a reader deletes the sentence, do they lose a fact they cannot recover
from the code?** If yes, keep it — past tense is fine. If no, delete it.

`config/detekt/detekt.yml`'s `excludes` globs and `multiplatformTargets` key are functional YAML,
not prose — a dead glob entry there is a config question, not this epic's.
