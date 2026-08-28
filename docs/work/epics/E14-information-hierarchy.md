# E14 — Information hierarchy

**Spec:** the "Propuesta" page of the JustChill Inicio canvas — https://claude.ai/code/artifact/f1ebb4ce-5235-4b60-846f-8699d6fc0e1f

## Why

A design audit run on the emulator (2026-08-28) found the visual system sound and the product layer
generic: three equal numbers where `DESIGN_SYSTEM.md` §1.1 demands one hero, rows titled with what
the user did not write ("Sin descripción"), a "saldo" that is a monthly net, and destinations
reachable from two places. The author kept the tokens and asked for the distribution, the
information shown and the navigation to change. This epic does that, one screen per ticket.

## Constraints

- **The tokens are not on the table.** Every ticket here renders with `EmmColors`, `EmmType`,
  `EmmSpacing` and `EmmRadii` as they are; a screen that needs a new value is a `DESIGN_SYSTEM.md`
  amendment first, never a literal in the screen.

- **A screen's summary has one hero amount.** One value in an `amount*` hero role; the others step
  down to `textSecondary` on one line (`DESIGN_SYSTEM.md` §1.1). Adding a second hero-sized number
  to a screen reintroduces the exact failure this epic removes.

- **The bottom bar's slots are fixed by E06.** Four tabs plus the centre add button; this epic only
  relabels them. A new destination swaps a tab out, it never appends one.

- **Removing a row from a screen can remove the only door to a route.** E06 records this twice.
  Before deleting a `ProfileScreen` or `AccountsScreen` row, `rg` the route it pushes and confirm a
  second door exists or the route is dead.

- **A transaction row is titled by what the user wrote, then by the category — never by a
  placeholder (E14-01).** `TransactionUi.title`/`subtitle` derive it in `:presentation`
  (`TransactionUiTest` pins the fallbacks); a composable that reintroduces "Sin descripción" or
  reads `description` directly is the defect this ticket removed.

- **`SeeTransactionsViewModel` sits at detekt's constructor cap (E14-01).** `detekt.yml` sets
  `LongParameterList.allowedConstructorParameters: 6` and the class has six. A datum the screen needs
  next joins through the query (`completeTransactions` already carries the category and the account
  name), not through a seventh constructor parameter.

- **The screen's 48dp header targets keep their glyphs on the rows' 24dp column by giving the
  padding back at the edge, not by shrinking the target (E14-01).** `SeeTransactionsHeader` derives
  the inset from `spacing.s12 - spacing.s5`; a literal that "tidies" it to a smaller box reopens
  the `DESIGN_SYSTEM.md` §4 violation the review caught.

- **A child of a fixed-height row is NOT 48dp tall by inheritance (E14-02).**
  `Alignment.CenterVertically` measures children at intrinsic height, so a clickable link inside a
  48dp band gets a ~20dp touch box unless it carries `fillMaxHeight()` itself — `FormMetaRow`'s
  `DateAction`/`NoteAction` are the pattern. Dropping that modifier passes every gate and reopens
  `DESIGN_SYSTEM.md` §4.
