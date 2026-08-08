# git-hooks

Tracked git hooks for this repo. Not active by default after clone — opt in with:

```bash
git config --local core.hooksPath scripts/git-hooks
```

Run once per clone.

## Hooks

| Hook | What it does | Skip with |
|---|---|---|
| `pre-push` | Runs `./gradlew detekt`. Blocks push if any new finding (not in `config/detekt/baseline-*.xml`). | `git push --no-verify` |

## ⚠️ This hook covers far less than it looks like

`./gradlew detekt` is the plain detekt task, and after the KMP migration it is
**`NO-SOURCE` on every KMP module**. Verified:

```
> Task :detekt           NO-SOURCE
> Task :data:detekt      NO-SOURCE
> Task :shared-ui:detekt NO-SOURCE
> Task :domain:detekt    NO-SOURCE
> Task :androidApp:detekt          ← the only one that actually runs
```

`:androidApp` is now a thin entry point, so the hook lints roughly nine files and
misses `:shared-ui` commonMain — which is where all the UI, ViewModels and Koin
wiring live.

Real coverage comes from the source-set tasks in the reinforced gate
(`docs/kmp/ORCHESTRATION.md`):

```bash
./gradlew detektMainAndroid                          # commonMain + androidMain, with type resolution
./gradlew detektIosMainSourceSet                     # iosMain, no type resolution
./gradlew :androidApp:detektMain                     # androidApp, all variants
./gradlew :shared-ui:detektAndroidHostTestSourceSet  # shared-ui host tests
```

Making the hook run those four instead is an open task. It is deliberately not
done yet: the KMP source sets have never been gated on push, so switching the
hook over will start blocking pushes until any findings they surface are either
fixed or baselined.
