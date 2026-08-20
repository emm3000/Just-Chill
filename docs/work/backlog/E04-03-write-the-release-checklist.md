# E04-03 — Write the release checklist

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] a checklist file exists and is linked from the docs map
- [ ] it carries the QA passes: clean install, a week offline-first, late sign-in, sign-out, and
  a real upgrade over an old APK with `adb install -r`
- [ ] it carries the restore drill that ADR 009 Decision 4 requires
- [ ] it names the Edit-commit step of `uploadRelease.yml` as what the release is judged by

## Context

ADR 009 Decision 4 requires a documented restore drill "on the release checklist". No such file
exists — `fd -i 'release|checklist' docs/` returns nothing — so the rule is currently prose.
