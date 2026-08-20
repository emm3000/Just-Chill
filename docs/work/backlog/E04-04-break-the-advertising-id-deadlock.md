# E04-04 — Break the advertising-ID deadlock

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] the Play Console advertising-ID declaration reads "No"
- [x] the checklist in `docs/PLAY_ADVERTISING_ID.md` has been re-run against the AAB that will
  actually be published, control test included
- [ ] a release reaches the alpha track with that declaration in place

## Context

The declaration was set to "Yes" to clear the rejection that stopped `v2.4.0`, and stayed. The
deadlock it created — flip "once `v2.4.0` has shipped", a release whose Edit was refused — is
broken: `v2.5.0` committed its Edit and left a draft on alpha. What remains is not repo work.
Publish that draft so it supersedes the release the conflict is suspected to live on, then flip.
