# E01-23 — Align the auth and backup mappings of the same exception

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [x] a generic `RestException` reaching `toAuthDomainException` is typed the way the backup path
  types it, or the divergence is written down as deliberate with the reason
- [x] a test pins whichever answer is chosen, so the two paths cannot drift apart again silently

## Context

E01-11 gave the backup path `DomainException.RemoteRejected(statusCode)` for a generic
`RestException`. `data/auth/DefaultAuthRepository.kt`'s `toAuthDomainException` still maps the same
family to `DomainException.Unknown`, so one exception now means two things depending on which caller
caught it.
Note storage-kt raises `UnauthorizedRestException` only for HTTP 400 carrying a `"401"` body field; a
plain 401 from the bucket arrives as `UnknownRestException`.
