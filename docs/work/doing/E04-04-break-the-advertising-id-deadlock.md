# E04-04 — Break the advertising-ID deadlock

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [x] the Play Console advertising-ID declaration reads "No"
- [x] the checklist in `docs/PLAY_ADVERTISING_ID.md` has been re-run against the AAB that will
  actually be published, control test included
- [ ] a release reaches the alpha track with that declaration in place

## Context

Play accepts "No" now: the rejection came from an old release still active in the `Internal
testing` track whose artifact carried the permission, and that track has been updated. `v2.6.0`'s
bundle is verified clean and sits as a draft on alpha; what remains is publishing it there.
