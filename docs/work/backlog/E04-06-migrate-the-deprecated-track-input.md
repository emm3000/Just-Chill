# E04-06 — Migrate the deprecated `track` input in uploadRelease.yml

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] `rg 'track:' .github/workflows/uploadRelease.yml` shows the input the action still supports,
      and a release run emits no deprecation annotation for it
- [ ] `actionlint` passes over the edited workflow

## Context

`v2.6.0`'s run annotated *"'track' is deprecated and will be removed in a future release. Please
migrate to 'tracks'"*. The action is pinned to a SHA, so nothing breaks until someone bumps it —
which is exactly when a release would fail, with the reason a year old. Confirm the replacement
input's spelling against the pinned action's own docs before editing; do not guess the plural.
