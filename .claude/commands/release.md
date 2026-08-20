---
description: Tag the current trunk HEAD as a release and upload a draft to the Play Store alpha track
argument-hint: "patch | minor | major | vX.Y.Z [--skip-tests]"
allowed-tools:
  - Bash
  - Read
---

You are executing the `/release` slash command. The user wants to tag the current trunk HEAD with a new semver tag, which triggers `uploadRelease.yml` on GitHub Actions. That workflow uploads the AAB to the Play Store alpha track **as a draft** — publishing it is a manual step in Play Console, so this command does not ship anything on its own.

## Parse arguments

The user invoked: `/release $ARGUMENTS`

Recognized forms, from `v1.6.0`: `patch` → `v1.6.1`, `minor` → `v1.7.0` (patch reset), `major` →
`v2.0.0` (minor and patch reset). An explicit semver is also accepted, with the leading `v` optional
— add it if missing. `--skip-tests` skips the local pre-flight run.

If the argument is missing or unrecognized, ask which bump or accept an explicit version. Don't
guess.

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

If the user gave `patch`/`minor`/`major`, read the latest **release** tag. The `--match` filter is
not optional — the same filter and the same reason are in `androidApp/build.gradle.kts`.
```bash
git describe --tags --abbrev=0 --match "v[0-9]*" 2>/dev/null
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
  Workflow:       uploadRelease.yml → Play Store alpha (status: draft — needs manual promotion)
```

Then ask the user to confirm with a clear yes/no. **Do not proceed without explicit confirmation.**

## Optional pre-flight tests

Unless the user passed `--skip-tests`, run the same tasks the CI `publish` job runs BEFORE tagging,
so a fail here predicts a CI fail without leaving a dead tag on the remote:

```bash
./gradlew qualityGate lintProdRelease
```

Do not substitute a hand-written task list: `qualityGate` is defined once in build-logic, and a list
spelled out here drifts from it silently until a release preflight fails for a reason that has
nothing to do with the release. On macOS `qualityGate` also compiles the iOS target; that is
intended.

If anything fails, STOP. Show the failure, tell the user to fix and rerun. Do not tag.

## Tag and push

Create an annotated tag:
```bash
git tag -a <new-tag> -m "Release <new-tag>"
git push origin <new-tag>
```

Do NOT push trunk itself here — pretests should have happened at the previous `uploadApk` cycle. Only push the tag.

## Post-release report

After the push succeeds, tell the user the tag is pushed and `uploadRelease.yml` is running, then
read `git remote get-url origin` and give them
`https://github.com/<owner>/<repo>/actions/workflows/uploadRelease.yml`.

Remind them what green does and does not mean:

- The workflow uploads the AAB as a **draft** on the alpha track. A green workflow alone reached
  nobody. Open Play Console → Pruebas → Alfa, confirm the draft's versionCode/versionName, publish.
- Only then: Play Console → Producción → "Promover desde otra pista" → la build de alpha → release
  notes → revisar → lanzar a producción. Google review takes 1–3 días for an existing app.

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
