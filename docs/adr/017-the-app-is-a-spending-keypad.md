---
status: accepted
date: 2026-09-20
---
# The app is a spending keypad

The owner runs this app every day and stopped recognising it: "se siente hecho
con IA, sin identidad". It is already minimal — dark, hairlines, no shadows,
mono amounts — and minimal is what every generated app looks like: grey on grey
inside cards, stock Material outlines, an orange accent that means nothing, rows
that read alike on every screen. Taking more away will not produce identity; one
decision only this app would take will. The owner named the Starlink Android app
as the reference: black, white, no decoration, one animated moment. The mockup
that settled the shape is https://claude.ai/artifact/TwsfcHXSc9oU1Ks3c6Wm5c, an
input to this decision and not a deliverable.

## Decision

- **Dark only, ground `#000000`.** No light theme and no toggle.
- **Monochrome.** The `accent` hue is removed. `success` is the only colour
  money wears and it marks income; the status tokens keep their system meaning;
  a Category's colour survives as an 8dp dot beside its name and nowhere else;
  icons are grey and never tinted.
- **Two families, one job each.** Inter for language; IBM Plex Mono with
  tabular figures for amounts only. The hero amount is 60–80sp.
- **Flat.** Rows sit on black, separated by a hairline or by space. No cards
  anywhere; `surface1`…`surface3` survive only for sheets and the pressed
  ground.
- **Home is the amount pad, and there is no bottom bar.** Only the amount is
  mandatory: the date defaults to today, and the type, account and category to
  what `GetFrequentCombosUseCase` ranks — a 90-day top-combos query as it
  stands, which W2 narrows by the typed amount and the hour — each editable
  through a chip under the number. A `±` key switches to income: the number
  turns green and takes a `+`. Save is one white full-width button, the only
  high-contrast element on screen. (ADR 021 replaces the `±` key with the
  `Ingreso`/`Gasto` pill and drops the `+`.)
- **The month screen leads with "gastado este mes".** Entró and neto sit under
  it in secondary, neto green with a `+` when positive; rows are flat, grouped
  by day. It is reached by tapping the month total on the pad, or swiping up.
- **A corner icon opens a flat list screen**: Reporte, Cuentas, Categorías,
  Recurrentes, Préstamos, Exportar, Importar, Acerca de. Pickers stay
  `ModalBottomSheet`, black and flat.
- **One signature motion.** On save the typed amount slides up and the month
  total absorbs it, about 400 ms, respecting `ANIMATOR_DURATION_SCALE`.

Report, loans, recurring, accounts and onboarding change through tokens only,
with no new layout until the pad and the month screen are closed. Execution is
waves over trunk, each a reviewed pull request that leaves the app usable on the
owner's phone with its real data: W1 tokens and atoms, W2 the pad as home with
no bottom bar plus the menu screen, W3 the month screen and the save motion, W4
the sweep.

## Consequences

`.claude/rules/ui-components.md`'s "One `accent` element per screen" rule
dissolves, because there is no accent left to spend, and issue #273 — accent
marks on the filtered list — dissolves with it. That file is rewritten here to
the target state; W1 lands it, and until then CLAUDE.md's final rule holds: the
code wins, and this doc is ahead of it.

`androidApp/CLAUDE.md`'s bottom-bar rule ("four tabs plus the centre add button,
no more") and `shell/AppBottomBar.kt` go in W2, and `AppNavHost` loses its
`BottomBarRoute` semantics with them; ADR 015's amendment that moved those
markers into `:core:ui` is narrowed, not reversed. The launcher's "anotar"
shortcut becomes redundant the moment the pad is the launch destination, and
stays only as the carrier for the frequent combos, the one thing it does that
the pad cannot.

No design document is created: the mockup is an input, and the tokens under
`:core:ui`'s `core/ui/theme/` plus emulator screenshots remain the entire design
contract, as the rules file already says. No PRD Won't-have row is amended
either — §1 bounds scope, not surface. §2's fifteen-second capture criterion is
the reason the pad is home, not a casualty of it.

## Considered options

- **Keep `#191919` with the orange accent and only reorganise.** The cheapest
  option, answering a complaint nobody made: the boxes were never the problem.
- **A light theme, or both themes.** Two themes double every token decision and
  halve the conviction of each. This app is used at night, on a phone, by one.
- **One typeface for everything, as Starlink does.** Cleaner, and it throws away
  the tabular mono on amounts, the one thing the app already did that a
  generated app does not. The mono stays, narrowed to amounts alone.
- **The transaction list as home with a bigger add button.** What every finance
  app does, and it puts a tap between opening the app and typing a number.
- **A two-tab bottom bar, pad and month.** A bar holding two items exists to
  justify itself; a swipe and a tap on the total reach the month without a
  permanent strip of chrome.
- **A bottom sheet for the menu instead of a screen.** Eight destinations, most
  of which push further; a sheet that deep is a screen with a worse back gesture.
- **One big `v3` branch.** Unreviewable, and it breaks the owner's daily driver
  for days on real data. Waves keep trunk shippable.
