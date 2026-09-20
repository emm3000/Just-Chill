---
paths:
  - "core/ui/src/main/kotlin/**"
  - "androidApp/src/main/kotlin/**"
  - "feature/*/src/main/kotlin/**"
---

# Shared UI rules

These rules state ADR 017's target — black, monochrome, flat — that W1 lands in `:core:ui`; until then the code wins, per CLAUDE.md's final rule.

The design system lives in `:core:ui`: the atoms under `core/ui/atoms/` (`com.emm.justchill.core.ui.atoms`) and the tokens under `core/ui/theme/` (`com.emm.justchill.core.ui.theme`). `core/ui/components/` holds legacy `Emm*` widgets that are **not** the design system. The navigation vocabulary is `:core:ui`'s `core/ui/navigation/` (`AppRoute` and its markers, `AppNavigator`, `NavHostBindings`, the `PlatformHostActions` interface), `AmountInputSheet` sits in `core/ui/sheets/` and `FormSection` is an atom. The app shell is `:androidApp`'s `shell/` (the nav host, the shortcut routes, the SAF host actions), not a feature package.

## The iron rule

Feature screens call **only** the repo's atoms. **Never** a raw Material3 control — no `Button`, `TextField`, `OutlinedTextField`, `Card`, `IconButton`, `Snackbar`, `TopAppBar`. The atoms for those are `FilledCta` / `OutlinedCta` / `StickyCTA`, `UnderlineTextField`, `IconBtn`, `showEmmSnackbar`, `JcTopBar`.

`Switch` has no atom: it is allowed only with `colors = emmSwitchColors(...)` from `:core:ui`'s `core/ui/atoms/EmmSwitch.kt`, never Material's default colours. `Text` and `Icon` have no atom; they are allowed only with a `LocalEmmType` role and a `LocalEmmColors` token, never an inline `TextStyle` or a literal color. `ModalBottomSheet` is the one Material3 container in use, always with `SheetDragHandle`.

A custom component written inside a screen never replaces an atom that exists for that purpose. If the atom does not fit, extend or modify it first.

## Before creating a component

1. **Check `core/ui/atoms/` first.** If it exists, use it. No exceptions.
2. **Decide the scope.** Used by a single screen, it belongs to its own `:feature:<name>` package, as a sibling file or under `components/`. Used app-wide, it belongs in `:core:ui`'s `core/ui/atoms/`.
3. **If it must be created**, template on `IconBtn.kt` (the 48dp target around a smaller glyph) or `FilledCta.kt`, name it by its role with a matching file name, and include a `@Preview` wrapped in `EmmTheme`.

## Theme tokens are the style guide

`EmmColors.kt`, `EmmType.kt`, `EmmSpacing.kt` and `EmmRadii.kt` hold every value, reached through `LocalEmmColors`, `LocalEmmType`, `LocalEmmSpacing` and `LocalEmmRadii`. There is no document holding hex codes or dp, and none should be created: a second copy drifts. This section says which token to reach for and why; the value is in the file.

Nothing enforces these rules mechanically: the gate sees Kotlin, not dp, and goes green on a 14dp tap target. Conformance is verified on the `medium_phone` emulator. A deliberate exception is stated at the site in one line naming the constraint; a screen that needs a new value adds a token, never a literal.

### Principles

1. **Numbers are the hero.** The amount is what the user came for. The `amount*` roles are the largest type in the app and the only ones in the mono family. A screen's summary has **one** hero amount, 60–80sp; the others step down to `textSecondary` on one line.
2. **Negative space is a component.** Whitespace has a name (`EmmSpacing`), a size and a reason. Crowding is a design failure.
3. **Hierarchy through type and tone, never through hue.** Emphasis is a step down the text ladder (`textPrimary` → `textSecondary` → `textTertiary`) or a change of size and weight. Hue is never decoration: `success`, the status tokens, the category dot, nothing else.
4. **Positive is tinted; negative stays monochrome.** An income amount takes `success`; an expense stays `textPrimary`, never red. `danger` means destructive or broken, not "money leaving". A signed net or balance aggregate (a month net, a total owed) follows the same rule: positive takes `+` and `success`, zero or negative stays monochrome. A magnitude under its own label ("Por cobrar", "Entran") is not a net and keeps the unsigned income/expense semantics. A new net or balance routes its sign and tint through `Money.positiveMoneyFormatted()` (`:core:ui`) and `AmountTone.color()` (`:core:ui`'s `core/ui/atoms/AmountTone.kt`); an inline `if` there reopens the monochrome-positive bug.
5. **Hairline over surface.** Rows sit on `bg` and separate with space or a 1dp `border` hairline, never by being lifted onto a lighter ground. No card, no elevation shadow; keep it that way.

### Colour

- **Surface** (`bg`, `surface1`…`surface3`, `border`, `borderFocus`): `bg` is black and is the ground of every screen. The steps above it are for a sheet and a pressed row, never for containing content.
- **Text** (`textPrimary`, `textSecondary`, `textTertiary`, `textDisabled`): the ladder is the whole hierarchy. The app's one white surface, a screen's primary button, labels itself in `bg`.
- **Colour is a datum, never a style.** A hue says something the data said, not something a designer chose: `success` for money in, a status token for a system state, the dot for a category. There is no brand hue to spend.
- **Status** (`success`, `warning`, `danger`, `info`, `posMuted`, `negMuted`): system state, plus `success` for income. The `*Muted` washes are the ground behind an icon or inside a `Pill`; the readable mark on top is the full-strength token.
- **Category** (`cat*` tints, `catGraphite` the fallback): an 8dp dot beside the category's name, never an icon tint or a surface. The domain stores a colour *name*; `:feature:report`'s `ReportFormat.kt` maps it to a token, so the palette retunes without a migration.

### Typography

- Two families split by job: Inter for language, IBM Plex Mono with `tnum` for every `amount*` role, both bundled. Pick a role by intent: `amountHero` / `amountL` for the one number a screen exists to show, `amountLead` for the amount a row leads with, `amountM` / `amountS` where the label leads; `display` / `headlineL` / `headlineM` for titles, `titleL` / `titleM` for section headers, `bodyL` (default) / `bodyM` for running text, `labelL` / `labelM` for buttons and chips, `caption` for metadata, `eyebrow` only through the `Eyebrow` atom.
- Number formatting is owned by `:core:ui`'s `NumberFormatEs.kt` and `CurrencyFormat.kt` and pinned by their golden tests: comma thousands, dot decimals (es-PE, hardcoded), `S/` before the number with one space, the sign before the symbol (`+S/ 1,234.56`), the Unicode minus `−` never the hyphen, two decimals always. In a hero amount the prefix and decimals are deemphasised so the integer part carries the glance.
- Italics are reserved for a secondary meta label (the `Variable` marker, a note). Never an amount.
- Forbidden: all caps outside `Eyebrow`; text sized in `dp`; more than two weights on one screen; mixed alignment inside one vertical column.

### Spacing, radii, elevation, icons

- Base unit 4dp: `EmmSpacing` `s0`…`s12`. Screen horizontal padding `s4`, never less; `s6` between sections of distinct purpose.
- Touch targets are 48×48dp, non-negotiable. A child of a fixed-height row is not 48dp by inheritance: `Alignment.CenterVertically` measures at intrinsic height, so a clickable inside a 48dp band carries `fillMaxHeight()` itself. A 48dp header target keeps its glyph on the rows' column by giving the padding back at the edge, never by shrinking the target.
- `EmmRadii` `r0`…`rXXL`, `rLTop` for sheet tops, `rFull` for circles. Default to the smallest radius that reads right. No shadows: a modal that must read as "above" gets `surface1`, a hairline and rounded top corners.
- `Icons.Outlined.*`; filled only when the icon represents a state. 24dp standard, 20dp inline with body text, 32dp rare. An icon is `textSecondary` or `textTertiary`, never tinted: an account shows its type's icon in grey. Never a brand logo or a bank's registered colours. The icon and label maps stay in `:feature:account`'s `AccountPalette.kt`.

### Component invariants

- There is no card. Rows sit directly on `bg`; a group is separated by space or a hairline and named by a header above it.
- Text inputs are underline-only (`UnderlineTextField`): `border` at rest, `borderFocus` on focus, `danger` on error with the helper text matching.
- A screen's primary action is one full-width white button, the only high-contrast element on it. A destructive action never takes it: `danger` text and a `danger` hairline on a transparent ground.
- Bars and charts have no track; the bar decorates and the row carries the meaning (`contentDescription` on the row, the graphic hidden from TalkBack).
- A transaction row is titled by what the user wrote, then by the category, never by a placeholder; `TransactionUi.title` / `subtitle` derive it in `:core:ui`.
- No screen shows an account "balance": accounts have no opening balance, so every per-account figure is a month-scoped net and says so.
- A screen never applies window insets itself: `AppNavHost`'s `Scaffold` owns them and hands them down as `PaddingValues`. `ModalBottomSheet` is its own window and the one exception.

### Interactive states

- **The press is acknowledged at finger-down**, from the same `MutableInteractionSource` the `clickable` takes: a shaped control — pill, chip, tile, circle, button — draws `ripple()` clipped to its shape (`:core:ui`'s `core/ui/atoms/IconBtn.kt`); a full-width row on `bg` swaps its ground to `surface1` through `collectIsPressedAsState()` (`:feature:profile`'s `ProfileRow.kt`, `ProfileRowWithTrailing`, and `:feature:category`'s `CategoriesScreen.kt`) — that swap is what `surface1` is for. `indication = null` only where that same source is read in the same composable. One exemption: `core/ui/atoms/Segmented.kt`'s `SegmentCell`, whose selected fill repaints under the finger.
- **A navigable row ends with `Icons.Outlined.ChevronRight`** at `spacing.s4`, `textTertiary`, `contentDescription = null`, after the amount column when there is one — navigable meaning the tap pushes a screen that holds more rows. A row that opens an editor, a sheet or a dialog carries none, and a navigable row never ends bare. Never `onClickLabel` as the only hint: it reaches TalkBack and nobody else. The canonical glyph is the `ChevronTrailing` atom (`core/ui/atoms/ChevronTrailing.kt`); it fixes the size and the tint, and `enabled = false` switches the tint to `textDisabled`.
- **A disabled or loading control keeps its `clickable`** with `enabled = false` and `role = Role.Button`, so a screen reader still announces a disabled button; `core/ui/atoms/OutlinedCta.kt` and `core/ui/atoms/EmmDialog.kt` show that mechanism, and their `textDisabled` labels are a tracked gap. It never leaves layout (`core/ui/atoms/IconBtn.kt`). Its ground is `surface1` and its label `textTertiary` — an obligation no atom implements yet. Never `.then(if (enabled) Modifier.clickable(...) else Modifier)`, never `textDisabled` on the label that names the blocked action (that token is for the meta text beside it), never a control hidden because it is disabled.
- **A touch box larger than its artwork** takes `clickable(interactionSource, indication = null)`; the painted child takes `.clip(shape)` then `.indication(interactionSource, ripple())` before its padding (`:core:ui`'s `core/ui/sheets/DatePickerSheet.kt`, `ShortcutPill`; `DayCell` for the clipped ripple alone). The order is load-bearing: `clip` before `indication` or the ripple squares, `indication` before `padding` or the label sits outside it. `ripple(bounded = false)` is never used.

### Accessibility floor

Contrast ≥ 4.5:1 for text, ≥ 3:1 for large text, no token grandfathered. Every interactive element carries a `contentDescription` or `Modifier.semantics`; a decorative graphic is explicitly hidden. `+` and `−` are always paired with words TalkBack can read. Font scale and `ANIMATOR_DURATION_SCALE` are respected.

## Never

- A raw Material3 control in a feature screen.
- A literal color, `.sp` size or `.dp` padding outside `:core:ui`'s `core/ui/theme/`.
- A card, a tinted icon, or a colour that is not a datum; a status token never means a transaction type.
- An illustrated mascot in an empty state, or emoji as an icon: icons are vector assets or drawn paths.
- A `clickable` whose `MutableInteractionSource` nobody reads.
