# E04-04 — Break the advertising-ID deadlock

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] the Play Console advertising-ID declaration reads "No"
- [ ] the checklist in `docs/PLAY_ADVERTISING_ID.md` has been re-run against the AAB that will
  actually be published, control test included
- [ ] a release reaches the alpha track with that declaration in place

## Context

The declaration was set to "Yes" to clear the rejection that stopped `v2.4.0`, and stayed.
`docs/PLAY_ADVERTISING_ID.md` says the flip back should happen "once `v2.4.0` has shipped" — but
that release failed at the Edit commit and never shipped, so the two block each other.
