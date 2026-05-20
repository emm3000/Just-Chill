# git-hooks

Tracked git hooks for this repo. Not active by default after clone — opt in with:

```bash
git config --local core.hooksPath scripts/git-hooks
```

Run once per clone.

## Hooks

| Hook | What it does | Skip with |
|---|---|---|
| `pre-push` | Runs `./gradlew detekt` across all modules. Blocks push if any new finding (not in `config/detekt/baseline-*.xml`). | `git push --no-verify` |
