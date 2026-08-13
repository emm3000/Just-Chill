---
name: auditoria
description: "Trigger: auditoría, auditar, ila, agresiva, sin piedad, audit existing code. Adversarial audit of code that already exists (no diff): verdict MANTENER/REFACTORIZAR/REHACER plus a writer-ready plan."
license: Apache-2.0
allowed-tools: Read, Grep, Glob, Bash, Write
metadata:
  author: "emm"
  version: "1.2"
---

## Activation Contract

ACTIVATE: existing target, no fresh diff — feature flow, module, `.github/`, docs vs code, project state.

NOT here:
- fresh writer change → `docs/WORKFLOW.md` step 4 reviewer
- frozen target, blind dual judgment → `judgment-day`
- pending diff/PR/branch/path → `code-review`, `security-review`, `simplify`
- editing skill text → `skill-improver`
- NEVER `cavecrew-reviewer`/`cavecrew-investigator`: Haiku default, compressed output, breaks the Output Contract

## Hard Rules

- Evidence per finding: `file:line`; for an ABSENCE, the exact `rg`/`fd` pattern and scope. None → drop it.
- Lead with what is broken; no praise, no hedging.
- Severity `CRITICAL`/`WARNING`/`NIT` only (`docs/WORKFLOW.md`).
- Never edit or commit ANYTHING under audit — code, docs, config, skills. Sole permitted write: the Output Contract's audit file.
- Aggression never licenses inflated severity; unevidenced is worse than silent.
- Verify against real code, never memory or a doc's claim.
- Wrong premise → say so with evidence.
- Delegate to a general-purpose agent with `model` EXPLICITLY opus; never frontmatter-only (`docs/adr/007-*.md`).
- Main thread fixes the boundary; the auditor never asks — it states its assumption in one line.
- Spanish report; code, identifiers, paths as-is.

## Decision Gates

| Verdict | Condition |
|---|---|
| MANTENER | Boundaries and implementation are sound. Defects are local and fixable in place, however severe. |
| REFACTORIZAR | Boundaries are right, the implementation is wrong. The contract does not change. |
| REHACER | The boundaries or the contract are themselves wrong. Rewriting beats fixing in place. |

A local fix belongs in the finding's `fix` field; `## Plan` is only for REFACTORIZAR or REHACER.

## Execution Steps

1. Restate target, boundary, budget; read that `references/gates.md` row, then the code.
2. Findings: evidence, defect, consequence, fix.
3. Never re-raise a claim `docs/PROGRESS.md` records as failed verification.
4. Rank by blast radius; emit the verdict, plus the plan on REFACTORIZAR/REHACER.

## Output Contract

- `## Veredicto` — verdict plus one sentence.
- `## Hallazgos` — by blast radius; severity, evidence, defect, consequence, fix.
- `## Premisa` — only if the premise was wrong, with evidence.
- `## Plan` — only on REFACTORIZAR/REHACER; ordered steps, one writer unit each.
- `## Cobertura` — required if the budget was hit OR the target is the whole project: read vs unread.
- Persist only if the user asks or findings exceed five; else chat. Write a NEW `docs/audits/<target-slug>.md`; if it exists, do not write — report the collision, stay in chat.
- `docs/DATE_AUDIT.md` and `docs/archive/sync/AUDIT.md` are PRIOR audits: read-only inputs, never write targets.
- Nothing else: no reading summary, no pleasantries.

## References

- `references/gates.md` — reading order, budgets.
- `docs/WORKFLOW.md`, `docs/PROGRESS.md`, `CLAUDE.md` `## Gotchas`.
