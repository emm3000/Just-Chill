# Multi-session orchestration

How the owner runs several Claude Code sessions on this repo in parallel, and what the orchestrator session must do before dispatching work to them. Read this before dispatching a ticket to a peer session. Issues and pull requests as the board: ADR 013. This playbook as the way of working, Gema's adapted: ADR 014.

## The unit

One GitHub issue of `emm3000/Just-Chill` labelled `ready-for-agent`. The label vocabulary is `docs/agents/triage-labels.md`, the `gh` operations are `docs/agents/issue-tracker.md`, the open board is `gh issue list --label ready-for-agent`, never a list in a doc. The issue IS the work: a `Done when` list of falsifiable conditions plus at most 3 lines of context. Constraints that outlive an issue live in `.claude/rules/` and the module `CLAUDE.md` files. Git plus engram are the chronicle; a fact goes in exactly one place. New issues come from the `ticket-writer` agent, from the PRD and the ADRs; `mattpocock-skills:grilling` stress-tests a plan before it is ticketed.

## The loop

```
1. Orchestrator SCOPES the issue (inline, cheap) and picks the dispatch-log row
2. /wave boots one peer per ticket → WRITER   ── own worktree, branch, commits, PR that closes the issue
3. CI runs qualityGate on the PR
4. Orchestrator delegates → pr-reviewer       ── MERGE or FIX FIRST, on every PR
5. Orchestrator rebase-merges, appends the dispatch-log row, closes the cycle
```

## Roles

- **Owner** approves the first wave of a session and verifies model and effort with `/model` in each pane. Once a wave is fully merged the orchestrator starts the next one on its own. Peer sessions are booted only through the `/wave` skill, never by running `scripts/justchill-wave` by hand. Refer to a session with an `@` prefix in chat (`@loans`): it disambiguates the session from a feature or ticket of the same name.
- **Orchestrator** coordinates: dispatches via `SendMessage`, reviews, merges. It stays thin.
  - Delegate investigation and any artifact-producing work (tickets, specs, surveys, docs) to a subagent with an explicit model.
  - Do inline only routing state (`git status`, `git worktree list`, `gh issue/pr list`, `ListAgents`) and at most 1-2 files to decide. `:presentation` or a module boundary touched, run the leak grep in `.claude/rules/architecture.md`.
  - Report minimal: act on review/agent findings, tell the owner 1-2 lines and only decisions that are genuinely theirs. Merging a clean PR is normal practice, not a question.

## Model and effort

- Every dispatch states model and **one** explicit effort (low / medium / high, never a range), with a one-line reason. The row comes from the table in `.claude/skills/wave/SKILL.md`: pick the cheapest model and lowest effort that gets it right; reserve Opus for work where a mistake is silent or expensive (migrations, backup/restore, auth, the DI graph); low is enough where a wrong answer fails tests loudly.
- Fable is for architecture and design decisions only: identity, tokens, component rules, mockups, visual judgment. Reviews, implementation and doc checks go to Opus or Sonnet.
- `model` is explicit on every Agent call: the parameter overrides an agent's frontmatter `model:`, and omitting it silently runs the definition or session default.
- A session cannot see its own reasoning effort; `ListAgents` does not show it, and asking a session returns a guess. The owner verifies with `/model` in each terminal. The orchestrator cannot self-manage its own effort either: tell the owner when to raise it (a conflicting rebase, judging a migration) and when to lower it back.
- Skill routing: an unknown-cause bug gets `mattpocock-skills:diagnosing-bugs` first; reading legwork goes to `mattpocock-skills:research`; a diff that is not a PR gets `mattpocock-skills:code-review`; an architecture or domain decision loads `mattpocock-skills:domain-modeling`.

## Slicing and waves

- One slice = one small PR: a migration + domain change, or one screen, or one integration. A ticket naming more than 2 screens, or a migration plus a screen, gets split into sub-issues with `gh` first.
- If a session passes ~60% context without a PR, it commits, opens a partial PR, clears, and continues.
- Waves are ordered by dependency; parallelism is safe only within a wave (shared `:domain` or atom changes first, then independent slices). No wave carries two tickets touching the same module; a schema change (`.sq` / `.sqm`) is always a wave of one. A wave starts only after the previous one is merged.
- Until `:presentation` and `:ui-android` are deleted (ADR 015), a feature-extraction ticket does not count as touching those modules when it only deletes packages it alone owns, nor `:androidApp` when it only edits the wiring file the scaffold ticket created for that feature. Likewise, an untangling ticket does not count as touching `:core:ui` when it only adds files no other ticket in the wave edits. Drop this exception with those modules.
- Before dispatching tickets filed by an audit, re-verify each against current `trunk`; the finding may already be fixed.

## Dispatch prompt checklist

Every dispatch to a peer session must include:

- Issue number, docs to read first (the relevant ADRs, rules and module `CLAUDE.md`, prior engram memos), branch name, and the peer's **own** worktree path (see Isolation below).
- The line: *"The issue's acceptance criteria are the contract and win over any file list here; run every criterion check before opening the PR."* Prefer criteria phrased as a command with expected empty output.
- The file list (`fd -e kt`) and their tests, plus the pre-scoped blockers. For a move/refactor, a dependency-closure directive: transitive imports mapped from the consuming layer, non-platform dependencies co-moved, platform symbols hoisted to callbacks.
- Gates: `./gradlew qualityGate assembleDevDebug` green before every commit (the gate excludes the build on purpose; its definition: `.claude/rules/github-workflows.md`); conventional commits; no `Co-Authored-By` (a PreToolUse hook in `.claude/settings.json` blocks it); English code and docs, Spanish only for UI strings; `rg` / `fd` / `bat` / `sd`; gotchas saved to engram.
- For any ticket that adds or changes behavior (a use case, a ViewModel rule, a derived `UiState` field), an instruction to load `mattpocock-skills:tdd` and work red, green, refactor, writing the failing behavior test before the code. Pure layout or restyle tickets skip it.
- For any screen-touching ticket, a visual check on the `medium_phone` emulator: screenshot every changed screen with `adb exec-out screencap -p` and compare against `.claude/rules/ui-components.md`. Publish the screenshots on a branch named `assets/<N>-visual-check`, with the PR head short SHA in every file name (`home-<sha>.png`), and link them in the PR body with `raw.githubusercontent.com` URLs; the `gh` CLI cannot attach images to a PR. The assets branch is deleted when the cycle closes.
- An instruction to keep the slice small and stop and report instead of expanding scope.
- An instruction to open the PR with `Closes #N`, not merge it, not watch CI, and message the orchestrator the PR URL in 1-2 lines.

## Launching peers

- `/wave <issue numbers>` is the only entry point, whether the owner types it or the orchestrator invokes it for the next wave: it classifies each ticket with the skill table, states `@<name> #<n> <model>:<effort>` to the owner before booting (deviations from the table carry a one-line reason), runs `scripts/justchill-wave`, waits for the peers in `ListAgents` and dispatches. The skill lives in `.claude/skills/wave/SKILL.md`.
- `scripts/justchill-wave name:model:effort [...]` writes `~/.warp/tab_configs/justchill-wave.toml` with one pane per peer in a horizontal split and opens it with `open "warp://tab_config/justchill-wave"`. A Tab Config opens as a new tab in the active Warp window; Launch Configurations (`warp://launch/`) always open a new window, so they are not used.
- `scripts/justchill-session name model effort` creates the detached worktree `../justchill-<name>` from `origin/trunk` when missing, then runs `claude -n <name> --model <model> --effort <effort> --permission-mode bypassPermissions` inside it. The peer creates its ticket branch with `git switch -c`.

## Isolation: worktrees

- Every peer works in its own git worktree (`../justchill-<name>`, branched off `origin/trunk`), never in the owner's main checkout, which holds owner-only uncommitted files. All sessions open in the same folder by default, so a checkout there changes the branch under every other session.
- Before changing the state of any checkout, find out who is using it; an unexpected branch may be a live peer, not a leftover.
- Review and verification prompts are read-only on every existing checkout. If gradle must run on a branch, or a red/green check needs a source edit, use a throwaway worktree under the session scratchpad and remove it afterward.
- `git checkout trunk` fails inside a worktree while the primary worktree is already on `trunk`; use `git fetch` + `git switch -c <branch> origin/trunk` instead.
- Cleanup is part of closing the cycle, not a later chore (see Between tickets).

## Isolation: emulator

- `medium_phone` is the only AVD. Never boot a second one and never seed a database into it. A migration change runs `:data:connectedDebugAndroidTest` there before `trunk`.
- Because there is one emulator, screen-touching tickets in the same wave take turns: a peer installs with `./gradlew installDevDebug` only after the orchestrator confirms nobody else is mid-check, and reports when its screenshots are on the assets branch.
- Shut the emulator down (`adb emu kill`) when the review cycle closes, together with the worktree cleanup. Keep the AVD.

## Review cycle

- A session gets nothing new until its previous PR is reviewed, fixed and merged. Never queue two tickets in one dispatch.
- Two-axis review (standards vs. `CLAUDE.md` and `.claude/rules/`, spec vs. the issue), plus screenshots checked against `.claude/rules/ui-components.md`. Every PR gets one. Reviews of mechanical slices, such as restyles, docs and renames, run sonnet:medium. Reviews of screens, logic, migrations, backup/restore, auth and the DI graph run opus:high. Post-review fixes run sonnet:low.
- Every PR review is a fresh `pr-reviewer` subagent that the orchestrator launches, one per PR, with the model passed on the call. It is not a long-lived peer session. The subagent is read-only. It does not build the app or rerun the gate, because CI already did. It reads the diff, the issue and `gh pr checks`, and it pulls the screenshots locally with `git show origin/assets/<N>-visual-check:<file>`. It checks that the SHA in each file name matches the PR head, and then views the images. It boots `medium_phone` only when screenshots are missing, stale or suspicious. It follows the `mattpocock-skills:code-review` skill for the Standards and Spec axes, with the PR's merge-base with `origin/trunk` as the fixed point. It returns `blocking|minor` findings, one verdict (MERGE or FIX FIRST) and one cause word (`checklist`, `judgment`, `spec`) for the dispatch log.
- No design-doc PR order: `.claude/rules/ui-components.md` forbids a design document, so the tokens and the screenshots are the whole design contract.
- Merge is rebase-only, linear history, CI required. The orchestrator never blocks its own turn on `gh run watch`; it merges when the CI notification or the session's report arrives.
- Auto-merge (`gh pr merge --rebase --auto`) only works while checks are still pending; on a CLEAN PR GitHub refuses it. Check `gh pr view <N> --json mergeStateStatus` first: merge directly with `gh pr merge <N> --rebase` when CLEAN, arm auto-merge only while pending, and have the implementing session confirm with `gh pr view <N> --json state` once CI passes.

## Between tickets

- A cycle is closed only when all of this is done, in order. First the PR is merged. Then the peer leaves its worktree clean and removes it (`git worktree remove <path>`). Then its local branch is deleted with `git branch -D <branch>` (a rebase-merged branch never counts as merged, so `-d` refuses it) and `git worktree prune` runs. Last, any throwaway review worktree under a scratchpad is removed. The orchestrator checks `git worktree list` before reporting the session as free. If a peer is gone, the orchestrator removes the leftovers itself, but only after confirming that no live session uses them.
- Also at cycle close, the orchestrator appends one row to the Recent table of `docs/agents/dispatch-log.md` and folds anything beyond the last 20 rows into its Summary: PR, issue, table row, model:effort, first-review verdict and, on FIX FIRST, whether the cause was a checklist item, a judgment error or a thin spec. The table in `.claude/skills/wave/SKILL.md` is tuned from that log, not from opinion.
- Also at cycle close, the orchestrator asks whether the merged work made an architecture or domain decision, or changed a domain term. If it did, it files a small docs ticket that loads `mattpocock-skills:domain-modeling` and adds or updates the ADR in `docs/adr/`, `CONTEXT.md`, or both. An ADR that the shipped behavior contradicts is fixed the same way.
- A peer session carries exactly one ticket. When its PR is merged, the orchestrator closes the cycle and kills the pane: kill the peer's `claude` pid, then `kill -HUP` its parent `zsh`. It never sends a second ticket to the same session. When every PR of the wave is merged, it invokes `/wave` with the next wave's tickets.
- At session start, the orchestrator searches memory (`mem_search`) for past dispatch gotchas before the first dispatch of the session.
