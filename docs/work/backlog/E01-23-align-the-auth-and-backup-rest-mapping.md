# E01-23 — Align the auth and backup mappings of the same exception

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] a generic `RestException` reaching `toAuthDomainException` is typed the way the backup path
  types it, or the divergence is written down as deliberate with the reason
- [ ] a test pins whichever answer is chosen, so the two paths cannot drift apart again silently

## Context

E01-11 gave the backup path `DomainException.RemoteRejected(statusCode)` for a generic
`RestException`. `data/auth/AuthExceptionMapper.toAuthDomainException` still maps the same family to
`DomainException.Unknown`, so one exception now means two things depending on which caller caught it.
