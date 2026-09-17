---
name: auditoria
description: "Trigger: auditoría, auditar, ila, agresiva, sin piedad, audit existing code. Adversarial audit of code that already exists (no diff): verdict MANTENER/REFACTORIZAR/REHACER plus a writer-ready plan."
license: Apache-2.0
allowed-tools: Read, Grep, Glob, Bash, Write
metadata:
  author: "emm"
  version: "1.4"
---

## Activation Contract

ACTIVATE: existing target, no fresh diff — feature flow, module, `.github/`, docs vs code, project state.

NOT here:
- fresh writer change → the `pr-reviewer` agent (`docs/agents/multi-session.md` `## Review cycle`)
- pending diff/PR/branch/path → `code-review`, `security-review`, `simplify`
- NEVER `cavecrew-reviewer`/`cavecrew-investigator`: Haiku default, compressed output, breaks the Output Contract

## Hard Rules

- Evidence per finding: `file:line`; for an ABSENCE, the exact `rg`/`fd` pattern and scope. None → drop it.
- Lead with what is broken; no praise, no hedging.
- Severity `blocking`/`minor` only, the same vocabulary as `pr-reviewer`.
- Never edit or commit ANYTHING — code, docs, config, skills. The auditor writes no file in this repo; its output IS the report.
- Never run Gradle — no `qualityGate`, no compile task. A finding only a Gradle task can prove is UNPROVEN, plus the exact command for the main thread.
- Aggression never licenses inflated severity; unevidenced is worse than silent.
- Verify against real code, never memory or a doc's claim.
- Wrong premise → say so with evidence.
- Delegate to a general-purpose agent with `model` EXPLICITLY opus; never frontmatter-only (ADR 014).
- Main thread fixes the boundary; the auditor never asks — it states its assumption in one line.
- English report; code, identifiers, paths as-is. The verdict words stay `MANTENER`/`REFACTORIZAR`/`REHACER`.

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
3. Never re-raise a claim already recorded as failed verification — that chronicle lives in engram; `mem_search` the claim first.
4. Rank by blast radius; emit the verdict, plus the plan on REFACTORIZAR/REHACER.

## Output Contract

- `## Verdict` — verdict plus one sentence.
- `## Findings` — by blast radius; severity, evidence, defect, consequence, fix.
- `## Premise` — only if the premise was wrong, with evidence.
- `## Plan` — only on REFACTORIZAR/REHACER; ordered steps, one writer unit each.
- `## Coverage` — required if the budget was hit OR the target is the whole project: read vs unread.
- Persist only if the user asks or findings exceed five; else chat. Persistence is `mem_save` plus an Artifact, never a `.md` in this repo — an audit is chronicle, and the chronicle lives in git and engram. A finding that needs work becomes a GitHub issue the MAIN THREAD opens; the auditor only proposes it.
- Nothing else: no reading summary, no pleasantries.

## References

- `references/gates.md` — reading order, budgets.
- `docs/agents/multi-session.md`, `CLAUDE.md` `## Non-negotiable rules`.
