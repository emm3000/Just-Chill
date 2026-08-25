# E07 — Detekt baseline burn-down

**Follows:** E01-36, which made the baselines truthful — `baseline-data-main.xml` fell 577 → 6 once
generated code stopped being baselined.

## Why

`docs/CODE_QUALITY.md` gotcha 1: a baseline entry creates **zero** pressure on old code, and nothing
will ever ask for the burn-down. Until E01-36 the inventory was 92% dead entries, so it could not be
read as a to-do list at all. Now it can. One ticket per module, lowest layer first, so each unit
stays small enough to review: `:domain` + `:data` → `:presentation` → `:ui-android` → `:androidApp`.

## Constraints

- **A burn-down ticket may only ever SHRINK a baseline.** Adding an entry is out of scope by
  construction. If a violation is too expensive to fix, the entry stays and the closing commit says
  why — a survivor with a stated reason beats a rushed fix on the read path.
- **Fix the code, never relax the rule.** No threshold edited in `config/detekt/detekt.yml`, no new
  inline `@Suppress`. `docs/CODE_QUALITY.md` prefers a baseline entry over a `@Suppress` precisely
  because the baseline is inventoried in one file; a `@Suppress` hides in the source forever.
- **Never hand-edit a baseline.** Regenerate with the matching `detektBaseline*` task and commit what
  it writes. The tasks now honour the generated-source excludes (E01-36).
- **`DetektCreateBaselineTask` over-reports relative to `Detekt`** in detekt `2.0.0-alpha.6` — it has
  flagged `EmptyFunctionBlock` and `TooManyFunctions` the check task does not. Never commit a
  baseline for a source set whose check task reports zero findings; delete the file instead.
- **The stem files `baseline-{androidApp,data,domain}.xml` are not part of this track.** They belong
  to the plain `detekt` task, which is `NO-SOURCE` on the KMP modules and is not a gate.
- **A file-level rule entry is permanent amnesty at any size.** `TooManyFunctions:Foo.kt` carries no
  count, so the file is exempt forever. These are the entries worth spending the most effort on.
- **`:androidApp`'s 128 dev-flavor entries are out of scope until the sandbox decision is made.**
  `androidApp/src/dev/kotlin/experiences/` is a deliberate dev-only playground with its own
  `ShortActivity` in the dev manifest; prod ships an empty `experiencesModule` stub. Deleting the
  sandbox would close those 128 without touching app code, so that is a product call, not a
  lint call. `:androidApp`'s real debt is the 7 entries prod also carries.
