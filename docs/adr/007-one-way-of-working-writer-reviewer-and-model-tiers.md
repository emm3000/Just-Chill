# ADR 007 — One way of working, repo-wide: writer, reviewer, model tiers

- **Status**: Accepted
- **Date**: 2026-08-12
- **Deciders**: Edgardo Muñoz
- **Amends**: [ADR 003](003-freeze-ios-keep-the-compile-gate.md), Decision point 5 ("Drop the
  per-slice writer + reviewer (both Opus) requirement. One writer, reviewed inline.").

> Resumen (es): el ritual de writer + reviewer —dos agentes delegados, el reviewer en contexto fresco
> y sin haber escrito el código que revisa— vuelve para **todo el repo**, no solo iOS/KMP. El ADR 003
> lo retiró apoyado en "no hay usuarios"; el hecho nombraba el device del autor, pero la inferencia de
> riesgo que sacó de ahí lo descontó a cero — error que el propio repo corrigió tres días después
> (2026-08-11, commit `043cace`) — sí hay un device, el del autor, con data real y acumulada, y cada push a trunk
> le llega por Firebase App Distribution. Se agrega una política de tiers de modelo (Haiku ejecuta,
> Sonnet escribe desde una decisión ya tomada, Opus decide) y el desempate para código: Sonnet donde
> el compilador o un test cachea el error, Opus donde no — hoy, la lista `## Gotchas` de `CLAUDE.md`.
> El resto del ADR 003 no se toca; el alcance queda en este repo, `~/.claude/agents/` sigue intacto.

## Context

ADR 003 (2026-08-08), Decision point 5: "Drop the per-slice writer + reviewer (both Opus) requirement.
One writer, reviewed inline." Its justification, in that ADR's Context: "That ritual was designed under the
belief that real users held real data on real devices. Fact 1 removes that premise. The ceremony is
now calibrated for a risk that does not exist." Fact 1, the premise doing the work, reads in full:
"There are no users. Not on Android, not on iOS. The closed Play alpha has no real installs beyond the
maintainer's own device. This contradicts what `docs/PROGRESS.md` claimed ('hay usuarios reales con
data en el device desde `4e6de6c`'); that claim is corrected in the same commit as this ADR."

Fact 1 was stated correctly — it names the maintainer's own device explicitly, and names that it
corrects `docs/PROGRESS.md`'s overclaim in the same commit. What was wrong is the inference ADR 003
drew from it one paragraph later: "the ceremony is now calibrated for a risk that does not exist"
discounts that same device to zero. `docs/PROGRESS.md` is explicit today: there are no *third-party*
users, but **the author runs the release build daily** via Firebase App Distribution, on a device
holding real accumulated data (`CLAUDE.md:13-15`); a push to trunk distributes to that device
(`docs/PROGRESS.md:44`). The doc names its own earlier failure mode on this exact point:

> "Primero afirmó 'hay usuarios reales con data en el device desde `4e6de6c`', que era falso y se
> corrigió el 2026-08-08. La corrección se pasó de largo: quedó como 'no hay usuarios', y de ahí se
> leyó una licencia para **romper data local sin costo para nadie**. No es cierto — hay un device con
> data que a nadie le gustaría perder." (`docs/PROGRESS.md:34-38`)

The 2026-08-08 date inside that quote is the same commit as ADR 003's Fact 1: the one that corrected
`docs/PROGRESS.md`'s original overclaim ("hay usuarios reales con data en el device desde `4e6de6c`")
and named the maintainer's device correctly. `043cace`, three days later, does not refute Fact 1 — it
corrects the inference drawn from it: `docs/PROGRESS.md:34-38` records that the 2026-08-08 correction
"se pasó de largo: quedó como 'no hay usuarios'," and from there licensed treating local data as free
to break. ADR 003 itself was never revisited afterward; it and the correction that undoes its risk
inference landed three days apart, in two documents that never cross-referenced each other until now.

"Green build, broken behaviour" is this repo's measured failure mode, not a hypothetical — three
instances, all found in production or by audit, none caught by the compiler or by `qualityGate`, each
cited to its own source below:

- **The predicate asymmetry that feeds the self-feeding sync loop in production.** Rows are counted
  on `syncState = 'Pending' AND userId IS NOT NULL`, but pushed only after an extra Kotlin-side
  `.filter { it.userId == userId }` — so a row with a stale `userId` is counted forever and pushed
  never, and nothing can clear it. That is the fuel: `markPendingForResync` fires an unconditional
  `notifyQueries`; `countPending` re-emits with no `distinctUntilChanged` anywhere in the chain; a bare
  `filter { > 0 }` ahead of a 3-second debounce re-triggers `runSync()` forever. Every cycle succeeds,
  and the `KeepLocal` path logs nothing, which is why it never reached Crashlytics (AUDIT §3, one
  causal chain — asymmetry, then loop).
- **The composite-primary-key mismatch that broke every push, silently, from 2026-06-14 to
  2026-08-12.** A migration was edited in place after it had already applied to production; production
  kept single-column primary keys while the client sent two-column `onConflict`, so every push failed
  `SQLSTATE 42P10`. Pull carried no conflict clause, so nothing on screen suggested a write was
  failing (`72a9b03`).
- **`delete_account` whose RPC never reached the network.** Zero `/rest/v1/rpc/*` requests and zero
  `postgres_logs` executions in the full window — a client-side failure before the HTTP call. The
  leading, still-unconfirmed candidate is a confirmed intent silently discarded at
  `ProfileViewModel.kt:100` (`if (currentState.op != ProfileOp.None) return`): no effect, no snackbar,
  no state change, no logging of its own (AUDIT §8 — settling it against the runner-up candidate needs
  the owner to say what actually appeared on screen).

ADR 003's cost argument is not in dispute here and is not being re-litigated: per-slice ceremony has
real cost, and that stays true. What this ADR revisits is only the risk side of that ledger — the
premise that the risk was zero.

## Decision

1. **One way of working for the whole repo, not per track.** Writer and reviewer are separate
   delegated agents; the reviewer runs in fresh context and never wrote the code it reviews. This
   applies uniformly to iOS SwiftUI slices, the sync redesign, bugfixes, and docs alike — the per-track
   split (frozen-iOS ceremony vs. everything else) is retired. **The reviewer is always Opus — no
   exception, no carve-out, regardless of blast radius.** Its entire job is the zone where the
   compiler, the tests, and `qualityGate` catch nothing; lowering its tier removes its reason to exist.
   It also reads and reports rather than writing, which makes it the cheapest agent in the loop to run
   at the top tier. Evidence from this ADR's own changeset: docs-only is the lowest blast-radius class
   the tier policy defines — exactly where a Sonnet reviewer would be permitted under a blast-radius
   carve-out — and an Opus reviewer in fresh context found four SEVERE findings in it, including that
   `docs/WORKFLOW.md` was matched by `.gitignore`'s `docs/*` rule and would have been deleted from the
   tracked tree on commit, while twelve tracked files referenced it. Two prior Sonnet-tier passes over
   the same changeset did not surface it.

2. **Model tiers.**

   | Tier | Role |
   |---|---|
   | Haiku | Runs and reports — gate/tests/builds/git, zero judgment, under a strict output contract (exit code + failing task names verbatim, nothing interpreted) |
   | Sonnet | Writes from a decision already made — docs, mechanical refactors, renames, applying an already-designed diff, tests from a spec |
   | Opus | Decides — architecture, ADR content, plans, reviewers, judges, and code where nothing else catches the error |
   | Main thread | Decides, delegates, verifies conclusions — never reads raw tool output |

3. **Tiebreaker for code writers.** Sonnet writes where the compiler or a test catches the error;
   Opus writes where nothing does. In this repo that list is the `## Gotchas` section of `CLAUDE.md`.

4. **Judgment Day's two-judge blind panel is Opus, with `model` passed explicitly on every `Agent`
   call.** This Sonnet carve-out applies only to that panel — never to the per-unit reviewer defined in
   point 1, which stays Opus unconditionally regardless of blast radius. Within Judgment Day, Sonnet
   judges only when blast radius is low (UI, formatters, tests, docs) — never because the diff happened
   to be small. Diff size does not predict risk: the composite-PK migration (`72a9b03`) was one small
   SQL file and rewrote production primary keys irreversibly, while a several-hundred-line Compose
   screen rewrite is fully reversible. Opus judges are mandatory when the change touches Supabase or
   SQLDelight migrations, auth, the surface exported to iOS (`:presentation` commonMain), or `.github/`
   (signing keys and credentials).

5. **Scope is this repo only.** The global agent definitions under `~/.claude/agents/` are deliberately
   NOT changed. Consequence: `jd-judge-a.md:7` and `jd-judge-b.md:7` carry `model: sonnet` in
   frontmatter; the `Agent` tool's `model` parameter overrides frontmatter, so omitting `model` on a
   call silently downgrades a judge to Sonnet — always pass it explicitly.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Keep ADR 003 Decision 5 as-is** | Rests on a risk inference — "a risk that does not exist" — that discounts the maintainer's device Fact 1 itself named; the repo documented that inference as wrong three days after the decision was made. |
| **Edit the global `~/.claude/agents/` frontmatter** | Out of the requested scope for this repo; the repo-scoped equivalent means copying 72 lines of agent definition to change one word, which then drifts the next time the plugin updates. |
| **Reviewer inline in the main thread** | The writer effectively reviews its own work, and it is exactly what pollutes the orchestrator context the tier policy exists to protect. |
| **Restore ADR 003's original two-Opus ceremony** | Not proposed — see Consequences: this ADR prices a Sonnet writer plus an Opus reviewer, not two Opus agents. |

## Consequences

### Positive
- A fresh-context reviewer that never wrote the code catches what the writer — and the compiler —
  both miss, which is exactly the class of bug this repo has now hit three times without a single
  compiler or gate signal.
- One policy for the whole repo instead of a per-track exception that has to be remembered and
  re-justified per slice.
- The model-tier table gives every future delegation a default instead of re-deciding it each time.

### Negative / costs
- **Slice cost rises versus ADR 003**: two agents per unit of work instead of one.
- Reconciliation, and it matters: ADR 003 was pricing **two Opus agents** per slice. This ADR does not
  restore that price. Per Decision points 1–2, the default is a **Sonnet** writer plus an **Opus**
  reviewer — the reviewer unconditionally, per Decision 1 — with Opus writing only where nothing
  catches the error (the `Gotchas` list). The ceremony returns; its original price does not.
- ADR 003's other decision points are untouched by this ADR, except point 6. ADR 005 already
  superseded point 1's frozen-UI scope and the UI-relevant items of the thaw checklist (the modals,
  `startTab`), while explicitly keeping ADR 003's compile-gate invariant (point 2) and constraint 8
  intact. Point 6 — "Drop per-slice human simulator verification. Frozen means compile-verified
  only." — does not survive ADR 005 Decision 1 unfreezing iOS as a learning track: `docs/archive/swiftui/PLAN.md`
  now sets the live per-slice bar as the Android gate green, the iOS app compiling **and running**,
  trunk shippable (`PLAN.md:90-91`), and names `xcodebuild` / a manual Xcode run as part of the
  per-slice gate (`PLAN.md:122-124`). Points 3, 4, and 7 stand exactly as ADR 003 wrote them; this ADR
  changes only point 5.
- Global judge definitions (`~/.claude/agents/jd-judge-a.md`, `jd-judge-b.md`) still default to
  Sonnet. This ADR's Opus-judge policy holds only when `model` is passed explicitly on the Agent call
  — and the omission is not a hypothetical: of 16 `jd-judge-a`/`jd-judge-b` invocations in this
  project's session logs, 8 carry no `model` key and ran on the Sonnet default, including the
  2026-08-12 judgment day over the composite-primary-key repair migration (session `baafe251`) — the
  exact class of change this ADR's judge policy is meant to guard. Full count in `docs/WORKFLOW.md`'s
  Enforcement note.

## Notes

- `docs/WORKFLOW.md` is the living procedure this ADR formalizes — loop, reinforced gate, and the
  full model-tier policy. This ADR is the record of *why*; `WORKFLOW.md` is *how*, and is the doc that
  stays current as the loop evolves.
- `docs/archive/kmp/ORCHESTRATION.md` keeps the KMP slice ledger and landmines this loop was proved
  against; historical, not superseded by this ADR.
