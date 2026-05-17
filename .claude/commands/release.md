---
description: Tag the current trunk HEAD as a release and publish to Play Store alpha
argument-hint: "patch | minor | major | vX.Y.Z [--skip-tests]"
allowed-tools:
  - Bash
  - Read
---

You are executing the `/release` slash command. The user wants to tag the current trunk HEAD with a new semver tag, which will trigger `uploadRelease.yml` on GitHub Actions and publish the build to Play Store alpha.

## Parse arguments

The user invoked: `/release $ARGUMENTS`

Recognized forms:
- `patch` → bump the patch version (e.g., last tag `v1.6.0` → new `v1.6.1`)
- `minor` → bump minor, reset patch to 0 (`v1.6.0` → `v1.7.0`)
- `major` → bump major, reset minor/patch to 0 (`v1.6.0` → `v2.0.0`)
- Explicit semver like `v1.7.0` or `1.7.0` (`v` is optional; you'll add it if missing)
- Optional flag `--skip-tests` to skip the local pre-flight test run

If the user provided no argument or something unrecognized, ask which bump (patch/minor/major) or accept an explicit version. Don't guess.

## Safety preflight (do these checks in order, STOP on failure)

1. **On branch `trunk`?**
   ```bash
   git rev-parse --abbrev-ref HEAD
   ```
   Must return `trunk`. If not, stop and tell the user to switch.

2. **Working tree clean?**
   ```bash
   git status --porcelain
   ```
   Must return empty. If not, stop and report uncommitted changes.

3. **Up to date with `origin/trunk`?**
   ```bash
   git fetch origin trunk
   git rev-list --left-right --count origin/trunk...HEAD
   ```
   Both numbers must be 0. If local is behind, tell the user to pull. If local is ahead, tell the user to push first (so CI on uploadApk validates the trunk state before tagging).

4. **Tag does not already exist locally or remotely?**
   ```bash
   git ls-remote --tags origin "refs/tags/<new-tag>"
   git tag -l "<new-tag>"
   ```
   Both must be empty. If the tag exists, stop and ask the user for a different one.

## Determine the new tag

If the user gave an explicit version, normalize to `vX.Y.Z` (add `v` prefix if missing). Validate it matches `^v\d+\.\d+\.\d+(-[\w.]+)?$`.

If the user gave `patch`/`minor`/`major`, read the latest tag:
```bash
git describe --tags --abbrev=0 2>/dev/null
```
If no tag exists at all, default to `v0.1.0` for `minor`/`patch` or `v1.0.0` for `major`. Otherwise parse the existing tag, strip leading `v`, strip any `-suffix` (treat `1.5.0-alpha` as `1.5.0`), bump per the rule.

## Show plan and confirm

Before doing anything destructive, print:

```
Release plan
  Current HEAD:   <short SHA> <commit subject>
  Latest tag:     <last tag or "none">
  New tag:        <new tag>
  versionCode:    <commit count from `git rev-list --count HEAD`>
  versionName:    <new tag without leading v>
  Workflow:       uploadRelease.yml → Play Store alpha (status: completed)
```

Then ask the user to confirm with a clear yes/no. **Do not proceed without explicit confirmation.**

## Optional pre-flight tests

Unless the user passed `--skip-tests`, run a quick local validation BEFORE tagging — same targets the CI `verify` job will run, so a fail here predicts a CI fail without polluting the remote with a dead tag:

```bash
./gradlew :domain:test :data:testDebugUnitTest :app:testDevDebugUnitTest
```

If anything fails, STOP. Show the failure, tell the user to fix and rerun. Do not tag.

## Tag and push

Create an annotated tag:
```bash
git tag -a <new-tag> -m "Release <new-tag>"
git push origin <new-tag>
```

Do NOT push trunk itself here — pretests should have happened at the previous `uploadApk` cycle. Only push the tag.

## Post-release report

After the push succeeds, tell the user:

1. The tag is pushed; `uploadRelease.yml` should be running now.
2. Give them the GitHub Actions URL pattern (read it from the remote):
   ```bash
   git remote get-url origin
   ```
   Construct: `https://github.com/<owner>/<repo>/actions/workflows/uploadRelease.yml`
3. Remind them:
   - Wait for the workflow to finish green (verify + publish).
   - Open Play Console → Producción → "Promover desde otra pista" → seleccionar la build de alpha → release notes → revisar → lanzar a producción.
   - Google review: 1–3 días para apps existentes.

## Failure recovery

If anything fails AFTER the tag was created but BEFORE it was pushed, delete the local tag:
```bash
git tag -d <new-tag>
```

If the tag was pushed but you need to retract (e.g., the user realized something was wrong):
```bash
git push origin :refs/tags/<new-tag>
git tag -d <new-tag>
```
But only do this on the user's explicit request — pushing then deleting tags is visible in history and confuses collaborators.

## Rules

- Never push trunk as part of this command. Trunk should already be on origin before tagging.
- Never amend or rebase commits as part of this command.
- Never override safety checks without the user explicitly asking ("force tag despite uncommitted changes").
- Never auto-confirm; the user must approve the plan.
