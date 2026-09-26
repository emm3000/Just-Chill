---
status: accepted
date: 2026-09-25
---
# The list is home and the bar comes back

Amends ADR 017. Five days on the pad as home, behind a hamburger and with no
bottom bar, the owner stopped finding things: "me estoy perdiendo, prefiero como
estaba antes". The look holds; the wayfinding does not, for three reasons that
are not taste.

- **Hidden navigation.** The bar was the map: where I am, where I can go,
  always on screen. Eight destinations behind a corner icon are recall, not
  recognition. Cash App, the one mass-market app that opens on a keypad,
  keeps a five-tab bar under it.
- **No state on open.** Every ledger app opens on state (Nubank, Revolut,
  Monefy, YNAB, Wallet) and puts capture one tap away. Half the opens are to
  look, not to type; a blank keypad taxes that half.
- **No feedback after save.** The list showed the new row; the pad flies the
  amount into a total and stays blank, so "did it save?" costs a tap.

The mockup that settled the shape is
https://claude.ai/artifact/St7L3g4Vs1nhcCq2pk6LD4, an input, not a
deliverable.

## Decision

- **Home is Movimientos.** Top to bottom: the eyebrow `Gastado en <mes>`
  with a chevron that opens the month picker, the month's spend at
  `amountL`, `Entró` and `Neto` on one secondary line (neto in `success`
  with a `+` when positive), then the day-grouped list with a day total at
  the right of each header. The `MonthSummary` block and the today nudge go;
  this header replaces both.
- **The bottom bar comes back**, five slots: `Movimientos`, `Reporte`, the
  add key, `Cuentas`, `Más`. Active label `textPrimary`, inactive
  `textTertiary`, hairline above, black ground. The add key is a 48dp white
  square with a black plus, the only high-contrast element on the tab
  screens. The four tab roots are `BottomBarRoute`s; every pushed screen
  hides the bar.
- **The pad is the add action, not the home.** The add key pushes
  `AddTransactionRoute` full screen, laid out exactly as ADR 021 set it,
  with two changes: the menu button becomes a close `X` that pops, and a
  successful save pops back to the list, where the new row is the feedback.
  The month line stays on the pad and the save motion still lands on it.
  A tap on a row pushes the same screen as edit, as today.
- **The hamburger and the menu screen die.** `Más` is the fifth tab and
  holds what the menu held minus the four tabs: Recurrentes, Préstamos,
  Categorías, backup, Exportar, Importar, Acerca de, grouped under
  eyebrows. `Reporte` and `Cuentas` are tabs and leave `Más`.
- **The launcher shortcut still lands on the pad**, pushed over the list,
  so the fifteen-second capture keeps its zero-tap path.
- **Everything else in ADR 017 stands**: black ground, monochrome, Inter and
  Plex Mono, flat rows, sheets, the save motion. ADR 021's pad layout stands.

## Consequences

`androidApp/CLAUDE.md` gets its bottom-bar rule back ("five slots, no
more"), `AppNavHost` regains a bottom-bar slot and `:core:ui`'s navigation
vocabulary regains the `BottomBarRoute` marker, rebuilt, not restored from
history. `HOME_ROUTE` becomes `SeeTransactionRoute`. The pad loses
`onOpenMenu`; `ProfileRoute` is reached only through the `Más` tab. The
capture pad's screenshot matrix re-renders for the close key. ADR 017's
"Home is the amount pad, and there is no bottom bar" and "A corner icon opens
a flat list screen" are superseded by this ADR; its Considered options row
"The transaction list as home with a bigger add button" is now the decision,
with the reason it gave against it, one tap between opening and typing,
accepted: the tap costs under half a second and buys the map.

## Considered options

- **Revert to the 2026-09-19 shell.** Loses the ADR 021 pad, the one part
  of the redesign that records a movement faster than the old form.
- **Pad home plus a bottom bar**, as Cash App does. Keeps the zero-tap
  capture and still opens on nothing and saves into nothing.
- **A two-tab bar, list and pad.** A bar with two items exists to justify
  itself, and Reporte and Cuentas stay buried.
- **Balance per account on home.** That is what Cuentas answers; the home
  question is "how much have I spent this month" first and "what did I do
  today" second.
