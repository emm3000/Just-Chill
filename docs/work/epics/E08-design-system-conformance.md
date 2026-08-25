# E08 — Design system conformance

## Why

`docs/DESIGN_SYSTEM.md` states criteria — touch targets, tone, spacing, which token to reach for —
and **nothing enforces a single one of them**. detekt sees Kotlin, not dp; `qualityGate` goes green
on a 14dp tap target. The rules are therefore honoured by memory, and memory drifts: a grep for
`clickable` boxes under the stated 48dp floor found 13 files, one of them an atom. This epic closes
the gap between what the design system says and what the app renders.

## Constraints

- **The doc is the source of truth, not the code.** A conformance ticket fixes the code to match
  `DESIGN_SYSTEM.md`. Changing the rule instead is an amendment to that doc and needs its own reason
  written there — never a silent relaxation to make a ticket close.
- **A deliberate exception is stated at the site, not left to look like a bug.** Where a rule cannot
  be met, the closing commit says why and the code carries one line naming the constraint.
- **Conformance is verified on a device, not in a diff.** Touch target, contrast and spacing changes
  shift layout; a green gate proves only that it compiles.
