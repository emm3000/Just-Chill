# JustChill — Design System

> v0.1 · Living document. Every token here is the source of truth; implementations in `ui-android/src/androidMain/kotlin/com/emm/justchill/core/theme/` must mirror it.

---

## 1 · Principles

Five rules that govern every decision below. When in doubt, return here.

1. **Numbers are the hero.** The amount is what the user came for. Everything else recedes — labels, icons, even the type of transaction. Allocate the largest type, the cleanest space, and the most contrast to numbers.

2. **Negative space is a component.** Whitespace (well, *blackspace*) is treated as a deliberate element with a name, size, and reason. Crowding is a design failure, not a layout problem.

3. **Hierarchy through type and space — never through color.** Color carries semantic load (status, category identity). It does not carry visual hierarchy. If a heading needs to stand out, change its size or weight, not its hue.

4. **Monochrome unless there's a reason.** Default to white-on-black. Color is earned: status (success/error/warning), category identity, focus indicators. Income and expense are distinguished by sign (`+` / `−`), not by green/red.

5. **Hairline over shadow.** Dark UIs read shadows as smudges. Use 1dp hairline borders at low opacity (or no border at all) to separate surfaces. Never use elevation shadows.

---

## 2 · Color Tokens

### 2.1 Surface

| Token | Hex | Use |
|---|---|---|
| `bg` | `#000000` | Root background. The canvas. |
| `surface1` | `#0E0E0E` | Cards, list rows, bottom sheets. Default "raised" surface. |
| `surface2` | `#171717` | Elevated within a card (e.g. selected item, input field background). |
| `surface3` | `#1F1F1F` | Highest local elevation. Use sparingly. |
| `border` | `#262626` | Hairline dividers, input borders, card outlines. Always 1dp. |
| `borderFocus` | `#3D3D3D` | Border when an input is focused or a card is active. |

> No more than three surface levels visible on the same screen. If you need a fourth, the layout is over-nested.

### 2.2 Text

| Token | Hex | Use |
|---|---|---|
| `textPrimary` | `#FAFAFA` | Body, headlines, amounts. Never use pure `#FFFFFF` — it vibrates on `#000000`. |
| `textSecondary` | `#A3A3A3` | Captions, metadata (date, time, "from account X"). |
| `textTertiary` | `#6B6B6B` | Placeholder text, disabled labels, deemphasized info. |
| `textDisabled` | `#404040` | Disabled buttons, inactive controls. |
| `textOnAccent` | `#0A0A0A` | Text placed on top of `accent` (e.g. inside the FAB). |

### 2.3 Accent (single)

| Token | Hex | Use |
|---|---|---|
| `accent` | `#E8E8E8` | Primary action surface (FAB, primary button bg). Near-white, not pure. |
| `accentMuted` | `#737373` | Secondary actions, inactive tab indicators. |
| `accentFocus` | `#FFFFFF` | Selected tab indicator, focused input border, active state. |

> The brand operates in monochrome. There is no "brand blue" or "brand purple". The closest thing to a brand color is the contrast between `#000` and `#FAFAFA`.

### 2.4 Semantic (status, not type)

| Token | Hex | Use |
|---|---|---|
| `success` | `#7BB47B` | Successful sync, save confirmation. Muted green, not vibrant. |
| `warning` | `#D9A95C` | Pending state, attention needed. |
| `danger` | `#C57070` | Errors, destructive confirmations. Muted red. |
| `info` | `#7B9EC5` | Informational toasts. |

**Important:** these colors are *not* used to mark income/expense **in row-level UI**. A transaction list row is pure monochrome; the sign in front of the number does the work.

> **Total hero exception (Reporte)**. In `ReportScreen`'s Mes tab, the total
> hero amount is the *only* signal of type in that view (no sign, no list
> context). To prevent ambiguity at a glance, the integer part of the
> hero amount is tinted `success` (Ingresos) or `danger` (Gastos). The
> currency prefix and decimals stay `textTertiary`. This exception is
> scoped exclusively to the Reporte total hero — every other amount in
> the app remains monochrome.

### 2.5 Category palette (curated dark tones)

Six muted tones designed to coexist on `#000000` without competing. Saturation kept around 30-45%, lightness around 40-55%.

| Name | Hex | Tone |
|---|---|---|
| `cat.slate` | `#5A6B7A` | Dusty steel blue |
| `cat.sage` | `#6B8268` | Muted sage green |
| `cat.terracotta` | `#A87060` | Warm earth |
| `cat.mauve` | `#8A6F8C` | Dusty purple |
| `cat.ochre` | `#B89A5F` | Aged gold |
| `cat.graphite` | `#6E6E6E` | Neutral / "uncategorized" |

When rendering a category chip or icon: the color is the **icon tint**, not the background. Background stays `surface1`. The eye reads category from icon shape + color, never from a colored card.

---

## 3 · Typography

### 3.1 Fonts

- **Primary:** [Inter](https://rsms.me/inter/) (variable, weights 400-700). Use the OpenType feature `tnum` for tabular figures wherever numbers appear in lists or stacked layouts.
- **Numbers/amounts:** Inter with `tnum` enabled. **No separate font** — Inter's tabular numerals are sufficient and avoid the "two fonts on screen" complexity.
- **Fallback:** system sans (Roboto on Android), should not be visible if Inter loads correctly.

### 3.2 Scale

All sizes in `sp`. Line heights in `sp` (not multipliers). Tracking in em (Material 3 convention).

| Role | Size | Line | Weight | Tracking | When to use |
|---|---|---|---|---|---|
| `amount.hero` | 48 | 56 | 500 | -0.5 | The big number on AddTransaction, transaction detail. |
| `amount.l` | 28 | 36 | 500 | -0.25 | Account balance card. |
| `amount.m` | 18 | 24 | 500 | 0 | Transaction list row amount. |
| `amount.s` | 14 | 20 | 500 | 0 | Compact summaries. |
| `display` | 36 | 44 | 600 | -0.5 | Onboarding hero, empty-state titles. |
| `headline.l` | 28 | 36 | 600 | -0.25 | Screen titles (when not centered toolbar). |
| `headline.m` | 22 | 28 | 600 | 0 | Section titles within a screen. |
| `title.l` | 18 | 24 | 600 | 0 | Card titles. |
| `title.m` | 16 | 22 | 600 | 0.1 | List section headers. |
| `body.l` | 16 | 24 | 400 | 0.15 | Primary body text. Default. |
| `body.m` | 14 | 20 | 400 | 0.2 | Secondary body. |
| `label.l` | 14 | 20 | 500 | 0.1 | Button labels, tab labels (selected). |
| `label.m` | 12 | 16 | 500 | 0.4 | Chips, metadata badges. |
| `caption` | 11 | 16 | 400 | 0.5 | Timestamps, "hace 2 días". |

### 3.3 Number formatting rules

- Currency symbol **before** the number, separated by a thin space: `S/ 1,234.56`.
- For amount.hero and amount.l, the currency symbol uses `textTertiary` (deemphasize) and is one size smaller than the digits.
- Decimals always shown (`.00`) for currency. No "smart" hiding.
- Sign always present: `+1,234.56` for income, `−1,234.56` for expense (use the Unicode minus `−`, not hyphen `-` — it aligns with `+` width).
- Thousands separator: locale-aware (Spanish: `1.234,56` if user is in es-PE, otherwise per locale).

### 3.4 Forbidden

- All caps for anything longer than 2 words in body/title roles. (Allowed: short chip labels, button labels max 12 chars, and the `Eyebrow` role — see §7.13.)
- Italic. Never.
- More than 2 weights on a screen.
- Mixing text alignment in the same vertical column.

> **Eyebrow exception.** All caps is permitted exclusively in the `Eyebrow`
> atom (10sp, w500, 0.16em tracking, `textTertiary`), which labels a section
> with the role of a column header in a table — not as decoration. Up to
> ~30 chars with a `·` separator (e.g. `TASA DE AHORRO · 6 MESES`). Never
> use all caps anywhere else in the app.

---

## 4 · Spacing

Base unit: **4dp**. Every dimension on screen must be a multiple of 4.

| Token | dp | Use |
|---|---|---|
| `space.0` | 0 | No space (touching). |
| `space.1` | 4 | Tightest. Icon-to-label, chip internal padding. |
| `space.2` | 8 | Default tight gap. Within a card. |
| `space.3` | 12 | Between related items (e.g., label and input). |
| `space.4` | 16 | Default container padding. Default list-row vertical. |
| `space.5` | 20 | Comfortable. Between sections of a card. |
| `space.6` | 24 | Between unrelated sections on a screen. |
| `space.8` | 32 | Major separation. Above a section heading. |
| `space.10` | 40 | Hero spacing. Around the amount.hero. |
| `space.12` | 48 | Screen-edge breathing room when intentional. |

### 4.1 Container rules

- Screen horizontal padding: `space.4` (16dp). Never less.
- Card internal padding: `space.4` on all sides.
- Touch target minimum: **48dp** (Material accessibility floor — non-negotiable). For icon-only buttons smaller visually, use a 48dp tap area with the icon centered.
- Vertical rhythm: prefer `space.6` (24dp) between sections of distinct purpose.

---

## 5 · Radii

| Token | dp | Use |
|---|---|---|
| `radius.0` | 0 | Default. Most surfaces. |
| `radius.s` | 6 | Inputs, chips. |
| `radius.m` | 12 | Cards, list items, sheets. |
| `radius.l` | 20 | Bottom sheet top corners. |
| `radius.full` | 999 | Circular avatars, FAB. |

> Default to `radius.0` unless the element needs to feel "soft" or "tappable" (cards, inputs). Sharp corners reinforce the architectural feel; over-rounding makes it feel like a toy.

---

## 6 · Elevation

**There are no shadows in this design system.**

Separation is achieved through:
1. Surface color difference (`surface1` on `bg`).
2. Hairline border (`border`, 1dp).
3. Spacing (the most-used technique).

If a component absolutely needs to communicate "above" — e.g., a modal sheet — use `surface2` + 1dp `border` on the top edge only, with `radius.l` top corners. No drop shadow.

---

## 7 · Components

### 7.1 Buttons

| Variant | Background | Text | Border | Use |
|---|---|---|---|---|
| `Button.Primary` | `accent` | `textOnAccent` | none | Main confirmation. One per screen max. |
| `Button.Secondary` | `surface1` | `textPrimary` | 1dp `border` | Alternative actions. |
| `Button.Ghost` | transparent | `textPrimary` | none | Tertiary, in-toolbar actions. |
| `Button.Destructive` | transparent | `danger` | 1dp `danger` | Delete confirmations. Never primary-styled. |

- Height: 48dp.
- Horizontal padding: `space.5` (20dp).
- Radius: `radius.s` (6dp) for all.
- Disabled: bg → `surface1`, text → `textDisabled`, no border.
- Pressed: bg darkens 8% (use `Color.copy(alpha)` overlay).

### 7.2 Text Inputs

- Style: **underline only**, no filled background.
- Underline: 1dp `border` default, `accentFocus` when focused.
- Label: `label.m` above input (not floating).
- Input text: `body.l`.
- Placeholder: `body.l` color `textTertiary`.
- Error state: underline + helper text in `danger`.
- Vertical padding: `space.3` (12dp) inside the input area.

### 7.3 List items (transactions)

The single most important component. Layout:

```
┌─────────────────────────────────────────────┐
│  [icon]  Description                +12.50  │
│          Category · 14:30                   │
└─────────────────────────────────────────────┘
```

- Container: `bg` (no card). Rows separated by `space.0` (no divider) or `border` hairline if density is high.
- Vertical padding: `space.4` (16dp).
- Horizontal padding: `space.4` (16dp).
- Icon: 24dp, tinted with the category color from `cat.*`.
- Description: `body.l` `textPrimary`. Max 1 line, ellipsize.
- Metadata line: `caption` `textSecondary`, dot separator `·` with `space.1` around.
- Amount: `amount.m`, right-aligned, `tnum`, with sign.
- Pressed: bg → `surface1`.

### 7.4 Cards

When used (avoid for simple lists):
- Background: `surface1`.
- Border: 1dp `border`.
- Radius: `radius.m` (12dp).
- Padding: `space.4` on all sides.
- No shadow.

### 7.5 Bottom navigation

- Background: `bg` (not surface — the bar reads as part of the canvas).
- Top hairline: 1dp `border`.
- Height: 56dp (Material default).
- Active item: icon `accentFocus`, label `accentFocus` weight 600.
- Inactive: icon `textTertiary`, label `textTertiary` weight 400.
- The center "Add" item: icon `accent` color with no special background (per the minimalism rule — don't make it a "FAB inside a bar", let it sit equal but colored).

### 7.6 Top app bar

- Background: `bg`.
- Height: 56dp.
- Title: `title.l`, centered or leading (pick one and stick to it).
- Icons: 24dp, `textPrimary`.
- No bottom border by default. Add `border` hairline only when content scrolls beneath.

### 7.7 Dialogs

- Background: `surface2`.
- Border: 1dp `border`.
- Radius: `radius.m`.
- Padding: `space.5` (20dp).
- Title: `title.l`.
- Body: `body.m`.
- Actions: text buttons aligned right, `space.4` between.

### 7.8 Bottom sheets

- Background: `surface1`.
- Top radius: `radius.l` (20dp).
- Drag handle: 4dp tall, 32dp wide, `textTertiary`, centered, with `space.3` above and below.
- Content padding: `space.4` horizontal, `space.5` vertical.

### 7.9 Account preset visuals (peruanidad concreta)

The 7 preset accounts from US-03 / US-17 (PRODUCT_REQUIREMENTS.md) have
fixed visual treatment. **No registered-trademark colors, no logo imagery
— legal risk.** Use a monochrome outline icon by *type*, with a curated
`cat.*` tint per account.

| Account | Icon (Material Symbols Outlined) | Tint |
|---|---|---|
| Efectivo | `Wallet` | `cat.graphite` |
| Yape | `Smartphone` (or `AccountBalanceWallet`) | `cat.mauve` |
| Plin | `Smartphone` (or `AccountBalanceWallet`) | `cat.terracotta` |
| BCP | `AccountBalance` | `cat.slate` |
| BBVA | `AccountBalance` | `cat.ochre` |
| Interbank | `AccountBalance` | `cat.sage` |
| Scotiabank | `AccountBalance` | `cat.slate` |

> Bancos peruanos comparten el tipo (icon `AccountBalance`) y se
> diferencian por tint. Yape y Plin son wallets digitales (icon
> distinto). Efectivo es lo más neutral.

The icon's tint is the `cat.*` color; the surface stays `surface1`. Never
draw the account in a colored card — the eye reads the account from the
icon + tint, never from a colored surface (per §1.4).

For user-created accounts: default to `Wallet` icon + `cat.graphite`,
editable later from `AccountsScreen`.

### 7.10 Income-by-category bars (US-11 — the apuesta)

The signature visualization of the app. Used in `ReportScreen` to show
monthly income split by category.

```
┌──────────────────────────────────────────────────┐
│ Sueldo                                S/ 4,500   │
│ ████████████████████████████████             60% │
│                                                  │
│ Freelance                             S/ 2,000   │
│ ██████████████                               27% │
│                                                  │
│ Ventas                                  S/ 800   │
│ █████                                        11% │
│                                                  │
│ Otros                                   S/ 200   │
│ █                                             2% │
└──────────────────────────────────────────────────┘
```

**Layout per row** (Notion-style, single line + thin underline):
- Leading dot: 6dp circle, category `cat.*` tint. `space.2` (8dp) to the right.
- Label: `body.l` `textPrimary` left-aligned. Takes remaining width.
- Amount: `amount.s` (14sp, `tnum`) right-aligned, same baseline as label.
- Percentage: `caption` (11sp, `tnum`) `textSecondary`, `space.2` to the right of amount.
- Underline (decorative progress hint): 2dp tall, width = `percentage / 100 * rowWidth`, `radius.s`, category `cat.*` tint, `space.1` below the row baseline. **No track** — empty space on the right, not a `surface1` track. The bar is just a stripe under the label, not a full progress bar.

**Row spacing**: `space.3` (12dp) between rows.

**Container**: two variants.
- *Default* (bars are the only content of the section): no card, direct on `bg`, horizontal padding `space.4`.
- *Grouped variant* (bars share the section with a header + footer — e.g. ReportScreen Mes tab): wrap in §7.4 card with header `Eyebrow` ("POR CATEGORÍA") and right-aligned counter ("4 categorías", `body.m` `textSecondary`). Footer line at the bottom of the card: `caption` `textSecondary` with `count movimientos · Promedio S/ X` (see §7.14).

**Sorting**: descending by amount. Always.

**Comparison vs previous month** (top of report, single line):
- `body.m` `textSecondary` for context: *"Comparado con Abril"*
- Delta: `label.l` `tnum`, in `success` (if higher) or `danger` (if lower), e.g. `+12%` or `−5%`.

**Empty state** (no income this month):
- Use §7.11 empty state with copy: *"Aún no registraste ingresos este mes — anotá el primero y volvé al final del mes."*

**Motion**:
- Bars animate in on first render: width 0 → final, `motion.medium` (300ms), `easing.emphasized`, stagger 50ms per row.
- Re-renders (after month change): no animation, snap.

**Accessibility**:
- Each row has `contentDescription`: *"Sueldo: S/ 4,500, 60 por ciento del total"*.
- Bar itself is decorative (`Modifier.semantics { invisibleToUser() }`).

### 7.11 First-launch manifiesto (US-02)

Layout for the manifesto screen shown once at first install (and reachable
again from `ProfileScreen > Acerca de`).

```
┌────────────────────────────────┐
│                                │
│                                │
│   Tu plata no necesita         │
│   un dashboard.                │
│                                │
│   Necesita tu atención,        │
│   30 segundos al día.          │
│                                │
│   Sin login.                   │
│   Sin notificaciones.          │
│   Sin que te vendamos nada.    │
│                                │
│   Solo vos, tu plata,          │
│   y la verdad.                 │
│                                │
│                                │
│   ┌──────────────────────┐     │
│   │      Empezar         │     │
│   └──────────────────────┘     │
│                                │
└────────────────────────────────┘
```

**Layout**:
- Container: full screen, `bg`, horizontal padding `space.6` (24dp).
- Content vertically centered.
- Text block left-aligned (not centered — sentencias are harder to read centered).
- 4 estrofas: each is 1-3 lines. Between estrofas: `space.10` (40dp).

**Typography**:
- All 4 estrofas: `display` (36sp / 44sp line / W600 / -0.5 tracking).
- Color: `textPrimary` for first 3 estrofas, **`textSecondary` for the last estrofa** (*"Solo vos, tu plata, y la verdad"*) to make it feel like the speaker dropping their voice at the end.

**Button**:
- `Button.Primary` full width minus `space.6` padding.
- Label: "Empezar".
- Distance from text block: `space.12` (48dp).
- Distance from bottom of screen: `space.6` (24dp).

**No logo, no illustration, no animation on entry.** The copy is the hero.
If you want to add anything visual, you're missing the point — go back to
PRODUCT_DISCOVERY.md §3.5.

**Tone for "re-visit" mode** (when opened from ProfileScreen, not first
launch):
- Same screen. Button changes to "Volver" (back arrow on top-left also
  works as exit).

### 7.13 Vertical bar chart — 6-month income vs expense (US-22)

The trends visualization used in `ReportScreen → Tendencias` tab. Shows 6 months of income vs expense as side-by-side vertical bars.

```
┌──────────────────────────────────────────────┐
│ ENTRÓ VS SALIÓ        ● Entró   ● Salió      │
│                                              │
│ ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃       │
│ ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃       │
│ ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃    ┃ ┃       │
│ Dic    Ene    Feb    Mar    Abr    May       │
│                                              │
│ Promedio mensual    S/ 5,973 · S/ 4,240      │
└──────────────────────────────────────────────┘
```

**Why two muted earth tones and not green/red.** Per §1.4, income vs expense is **not** distinguished by color — the user reads "entró" / "salió" from the legend, not from the hue. We use `cat.sage` for income bars and `cat.terracotta` for expense bars. Both come from §2.5, so the chart stays inside the curated category palette.

**Layout per month group**:
- Two bars side by side, 12dp wide each, 4dp gap between.
- Income bar: `cat.sage` fill.
- Expense bar: `cat.terracotta` fill.
- Bar height: proportional to amount, max bar = full chart height (120dp by default).
- Track behind bars: none. Bars sit on `bg` (or `surface1` if inside a §7.4 card).
- Month label below: `caption` (11sp) `textSecondary`. Current month label is `textPrimary` w600 so the user finds "where am I now" without scanning.

**Group spacing**: equal gaps between month groups, calculated as `(chartWidth - 6 * groupWidth) / 7` so first and last groups have edge padding equal to inter-group gap.

**Chart container**: §7.4 card. `Eyebrow` header ("ENTRÓ VS SALIÓ") at top-left, legend dots at top-right (`cat.sage` dot + "Entró", `cat.terracotta` dot + "Salió", `label.m` `textSecondary`).

**Promedio mensual footer**:
- Below the chart inside the same card, separated by `space.4`.
- `label.m` `textSecondary` "Promedio mensual" left-aligned, amounts right-aligned `amount.s tnum`. Two amounts separated by ` · `, income first then expense.

**Motion**:
- Bars animate height 0 → final on first render, `motion.medium` (300ms), `easing.emphasized`, stagger 50ms per month group.
- Re-renders (after data refresh): snap.

**Accessibility**:
- Each month group has `contentDescription`: *"Mayo: entró S/ 6,200, salió S/ 4,800"*.
- Bars themselves are `invisibleToUser`.

**Early state (< 3 months of data)**:
- Don't render the chart. Use §7.12 empty state with copy: *"Vuelve cuando tengas más historial — Tendencias necesita al menos 3 meses para tener algo útil que mostrar."*
- CTA hidden (no action user can take to fix this — it's just time).

---

### 7.14 Report metrics — comparison pill + section footer (US-11 / US-22)

Two atoms used together on the Reporte screen.

**Comparison pill** (top of total, both tabs):
- Uses §atoms `Pill` with `tone = Pos` (up) or `tone = Neg` (down).
- Leading icon: `Icons.Filled.ArrowUpward` (pos) or `Icons.Filled.ArrowDownward` (neg).
- Copy format: `<sign-icon> S/ <absolute-delta> · <percent>%`, followed by separate body text `vs. <prevMonth>`.
- Example: `↑ S/ 660 · 12%` (pill) ` vs. abril` (`body.m textSecondary`, outside pill).
- Hidden when prior month total is 0 (no baseline to compare against).

**Section footer** (inside §7.10 grouped-variant card):
- Single line at the bottom of the "POR CATEGORÍA" card, separated from bars by `space.4` and an optional 1dp `border` hairline above.
- Left text: `<n> movimientos`, `caption` `textSecondary`.
- Right text: `Promedio S/ <amount>`, `caption` `textSecondary`, `tnum`.
- Average = total ÷ movement count. Rounded to 0 decimals.
- Singular form for 1: `1 movimiento`.

### 7.15 Empty states

```
            [outline icon, 48dp, textTertiary]

                  No tienes transacciones

                Las que registres aparecerán aquí

                    [ Crear transacción ]
```

- Icon: 48dp, `textTertiary`.
- Title: `headline.m`, `textPrimary`, weight 600.
- Subtitle: `body.m`, `textSecondary`.
- Action (optional): `Button.Primary` or `Button.Ghost`.
- Vertically centered in available space, max-width 280dp.

---

## 8 · Iconography

- Family: **Material Symbols Outlined** (filled only when an icon represents a *state*, e.g. heart-filled = favorited).
- Standard size: 24dp.
- Small: 20dp (inline with `body.m`).
- Large: 32dp (rare — empty states, hero illustrations).
- Stroke weight: 1.5dp (Material default for Outlined).
- Tint: `textPrimary` by default; `textTertiary` for decorative.
- Category icons inherit the category's `cat.*` tint.

---

## 9 · Motion

- Easing curves:
  - `easing.standard` = `CubicBezier(0.2, 0.0, 0.0, 1.0)` (Material standard).
  - `easing.emphasized` = `CubicBezier(0.05, 0.7, 0.1, 1.0)` (for major transitions).
- Durations:
  - `motion.short` = 200ms (button feedback, micro-interactions).
  - `motion.medium` = 300ms (sheet open, nav transitions, AnimatedVisibility).
  - `motion.long` = 500ms (rare — onboarding intros, hero entry).
- Bottom nav hide/show on secondary screens: 300ms enter / 250ms exit (already implemented in `ui-android/src/androidMain/kotlin/com/emm/justchill/hh/shared/AppNavHost.kt`).
- All motion can be disabled if `Settings.Global.ANIMATOR_DURATION_SCALE = 0`.

---

## 10 · Accessibility

Non-negotiable floor:

- Contrast ratios: text ≥ **4.5:1** against background, large text (≥18sp or 14sp bold) ≥ **3:1**. The token values above all clear this; if you ever introduce a new color, verify with a contrast checker.
- Touch targets ≥ 48×48dp.
- Every interactive element has a `contentDescription` or `Modifier.semantics`.
- `tnum` aside, never use a single character to convey meaning. `+` and `−` are paired with the textual context ("Ingreso", "Gasto") accessible to TalkBack.
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE` and the user's font-scale setting (every size in `sp`, never `dp`).

---

## 11 · Naming convention (Kotlin tokens)

When implementing in `ui-android/src/androidMain/kotlin/com/emm/justchill/core/theme/`:

```kotlin
object EmmColors {
    val bg = Color(0xFF000000)
    val surface1 = Color(0xFF0E0E0E)
    // ...
    object Cat {
        val slate = Color(0xFF5A6B7A)
        val sage  = Color(0xFF6B8268)
        // ...
    }
}

object EmmType {
    val amountHero = TextStyle(
        fontFamily = Inter,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.W500,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )
    // ...
}

object EmmSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    // ...
}

object EmmRadii { /* ... */ }
```

Exposed through `EmmTheme { ... }` via `CompositionLocal`s so any composable can read `LocalEmmColors.current.surface1`.

---

## 12 · Reference: what to look at

- **Starlink mobile app** — the architectural feel, monochrome discipline, status-not-decoration use of color.
- **Apple Wallet** — amount-as-hero typography, no card chrome.
- **Linear** — keyboard-first density, restrained accent usage.
- **Things 3** — list-item rhythm, generous padding.

Not references:
- Mint, YNAB, most personal finance apps. They lean colorful and chrome-heavy.
- Material 3 sample apps. We borrow the *scale* (sp values, spacing units) but not the *color philosophy*.

---

## 13 · Roadmap (implementation order)

Aligned with the sprints in `docs/archive/ROADMAP_V1.md` — all ten steps below landed, and that
roadmap is archived. Kept as the order the system grew in, not as a plan.

1. **Foundations layer** — `Color.kt`, `Type.kt`, `EmmSpacing.kt`, `EmmRadii.kt`. Import Inter font. (Already in place — verify against §2-5 before S1.)
2. **Core components** — Button variants, TextInput, ListItem (transaction row), Card. (Mostly in place — verify before S1.)
3. **`SeeTransactionsScreen`** — exercises ListItem + headers + empty state. (Done.)
4. **`HomeScreen`** — exercises amount.hero, balance hero, month navigation chevrons (added in S1 / US-09).
5. **`AddTransactionScreen`** — amount input, type toggle, category picker. (Done.)
6. **`ReportScreen` (US-11, S1)** — exercises §7.10 income-by-category bars, mes navigation. **This is the most important visual deliverable of v1.**
7. **`FirstLaunchScreen` (US-02, S2)** — exercises §7.11 manifesto layout.
8. **`ProfileScreen` polish (US-21, S4)** — full list of 7 items, ListItem reuse, no logo.
9. **`AccountsScreen` revamp (US-03 / US-17, S0 cleanup + S4)** — exercises §7.9 preset account visuals.
10. **Empty states audit (S5)** — every screen has a polished §7.12 empty state, no debug strings.

Each step ends with a commit that updates this doc if the implementation forced a token change.

> **Auth screens exist and this section used to deny it.** It said "Not in this roadmap: any Auth /
> Login / Register screen. The app has been local-only since the migration that removed
> Supabase/Ktor/auth." [ADR 001](adr/001-reverse-local-only-to-local-first-optional-sync.md)
> reversed that: the app is local-**first** with opt-in Supabase sync, and `AuthScreen.kt`,
> the CUENTA block in `ProfileScreen.kt` and Google sign-in all ship today.
>
> The now-archived `docs/archive/AUTH_SYNC_UI_BLUEPRINT.md` flagged this exact contradiction as its
> deviation #7 and it went unactioned for months, which is why it is spelled out here rather than
> quietly deleted. That blueprint is the screen-by-screen map of the auth/sync UI as built; the
> tokens it should be measured against are the ones in this doc. Auth has no section of its own
> here yet — that is the real gap.

---

## 14 · Change log

| Date | Change | Reason |
|---|---|---|
| 2026-05-16 | Initial draft | Rebrand kickoff (Starlink-inspired, monochrome) |
| 2026-05-17 | Alineado con Fases 1-4 del proceso de definición | Removed legacy Auth refs (§13). Added §7.9 account preset visuals (peruanidad concreta). Added §7.10 income-by-category bars (US-11 la apuesta). Added §7.11 manifesto layout (US-02). Renumbered §7.9 empty states → §7.12. |
| 2026-05-21 | Report redesign handoff alignment | §3.4 eyebrow exception (all caps permitted in `Eyebrow` role). §7.10 card variant added for grouped layout. New §7.13 vertical bar chart for Tendencias (income vs expense, `cat.sage` + `cat.terracotta`, NOT green/red — §1.4 preserved). New §7.14 comparison pill + section footer. Empty states renumbered §7.12 → §7.15. |
| 2026-05-21 | Report v2 — second designer pass | §1.4 total-hero exception added (`success`/`danger` tint on Reporte's hero integer part only). §7.10 row layout rewritten to Notion-style (dot + name + amount + %, thin colored stripe instead of full progress bar). |
