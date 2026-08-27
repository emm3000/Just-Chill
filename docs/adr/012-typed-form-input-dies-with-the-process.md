# ADR 012 — Typed form input dies with the process

- **Status**: Accepted
- **Date**: 2026-08-27
- **Deciders**: Edgardo Muñoz
- **Amends and supersedes**: nothing. No W- row moves and none is added — `PRODUCT_REQUIREMENTS.md`
  §1 has never carried a row about state restoration. §2's **Persistencia** row ("un crash o
  force-close no pierde data") stands untouched: it is about the SQLDelight ledger, and text still
  being typed into a form has not reached it and is not data yet.

> Resumen (es): lo que escribes en un formulario y no guardas se pierde si Android mata el proceso.
> No se adopta `SavedStateHandle`. Esa alternativa cuesta duplicar **permanentemente** siete campos
> en seis ViewModels — cada campo vive en su `UiState` y otra vez en el handle, y de cada campo nuevo
> hay que acordarse de espejarlo, para siempre — a cambio de rescatar unos segundos de tipeo. La
> única pérdida irreversible de esta app es la data guardada, y esa vive en SQLDelight, no en un
> `TextField`. Lo que sí se corrige: hoy la hoja sobrevive más que el campo que contiene, porque el
> flag está en `rememberSaveable` y el texto está en el ViewModel. El flag se muda al `UiState` para
> que viva y muera exactamente cuando vive y muere el campo.

## Context

1. **No ViewModel takes a `SavedStateHandle`** — `rg SavedStateHandle presentation ui-android androidApp`
   is empty. Nothing has ever been restored; this ADR decides whether that stays true.
2. **The exposure is six ViewModels and seven typed fields**: `AddTransactionViewModel`,
   `EditTransactionViewModel`, `AddEditLoanViewModel`, `AddEditRecurringMovementViewModel`,
   `AddCategoryViewModel` and `AddAccountViewModel`, holding `amount`, `description`, `personName`,
   `amountDigits`, `interestPercentText`, `note` and `name`.
3. **The forms are short and there is one user.** No third-party users, and the author runs the
   release daily on real data. A lost form is retyped in seconds; the failure this app cannot afford
   is losing a committed row, and that is `:data`'s obligation, not the form's.
4. **The sheet flags already disagree with the fields they contain.** `AddTransactionScreen`,
   `EditTransaction`, `AddEditLoanScreen`, `ReportScreen` and `SeeTransactionsScreen` hold sheet
   visibility in `rememberSaveable`, which the system restores after process death; the fields inside
   those sheets live in a ViewModel, which does not. A restored screen shows a restored sheet over
   empty fields — the app is already inconsistent with itself, in the direction this ADR rejects.

## Decision

1. **Typed form input dies with the process, and no ViewModel takes a `SavedStateHandle`.** The rule
   a future writer applies without re-reading this page: a `TextField` value belongs in the
   ViewModel's `UiState` and nowhere else. It survives a configuration change because the ViewModel
   does; it is gone after process death, and that is the accepted behaviour, not a bug to file.
2. **A sheet, a dialog, or any visibility flag the UI keeps rendering lives in `UiState` too.** This
   is E12's existing constraint ("an effect is a navigation or a transient message") applied to the
   third storage option. The reason is not testability alone: `UiState` is the only holder whose
   lifetime matches the fields the sheet contains.
3. **`remember` and `rememberSaveable` are both wrong for such a flag, for opposite reasons.**

   | Holder | Survives a configuration change | Survives process death |
   |---|---|---|
   | `remember` | no — rotation recreates the composition | no |
   | `rememberSaveable` | yes | yes |
   | ViewModel `UiState` | yes | no |
   | *the typed fields, for reference* | yes | no |

   `rememberSaveable` **over-survives**: the sheet outlives the fields inside it. `remember`
   **under-survives**: the sheet closes on rotation while those fields are still there. Only
   `UiState` matches the fields in both columns. Downgrading a `rememberSaveable` to `remember` is
   therefore not the alignment — it trades one mismatch for the other.
4. **Two deliberate exclusions in the auth screen, and both stay.** `AuthUiState.password` is never
   mirrored into anything that outlives the ViewModel — a credential in a saved-state bundle is a
   credential the system writes to disk. And `passwordVisible` stays a plain `remember`, not
   `UiState`: re-masking the password on a configuration change is the wanted behaviour, and
   `AuthScreen` already carries the comment saying so. It is the one flag whose correct lifetime is
   deliberately shorter than the field it describes.

## Alternatives considered

| Option | Why rejected |
|---|---|
| **Adopt `SavedStateHandle` across the six ViewModels** | The real alternative, and the platform's own answer. Rejected on cost: each of the seven fields would live twice — once in `UiState`, once in the handle — permanently, and every field added later has to be remembered into the mirror, with nothing mechanical catching the omission. What that buys is a few seconds of retyping, for one user, on a screen he can reopen. |
| **Delete the `rememberSaveable` flags down to `remember`** | The obvious-looking alignment, and it is wrong — see Decision 3. It replaces "the sheet outlives its fields" with "the sheet dies while its fields live", which is the same defect pointed the other way. |
| **Persist form drafts to SQLDelight** | Turns an unsaved form into stored data, which nobody asked for: a half-typed movement then needs a lifecycle, a discard path, and a place in the ADR 009 snapshot. |

## Consequences

### Positive

- One holder per piece of screen state. No mirror to keep in sync, no extra constructor parameter on
  six ViewModels, and every visibility flag becomes assertable through `state.value`.
- Context 4's mismatch disappears instead of being papered over.

### Negative / costs

- **A backgrounded form loses what was typed into it, silently.** Under memory pressure, or with
  "Don't keep activities" on, the user returns to an empty form and nothing explains why. Accepted,
  not mitigated.
- **The rule is a convention, not a mechanism.** Nothing rejects a `rememberSaveable` added to a
  screen tomorrow. [ADR 011](011-android-only-drop-the-kmp-build-and-the-ios-target.md) Decision 4
  already booked this class of cost for `:presentation`.
- Wanting restored forms later means adopting `SavedStateHandle` after all, across more ViewModels
  than the six named here.

## Notes

- The alignment is real work and is not this ADR: `SeeTransactionsScreen`'s confirm sheet is E12-04,
  the remaining flags are E12-05. The constraint that outlives both tickets lives in
  [`docs/work/epics/E12-mvi-core.md`](../work/epics/E12-mvi-core.md).
- Sheet-internal draft state (`AmountInputSheet.draftDigits` and its kin) is a sheet's own scratch
  space, not screen state, and this ADR does not decide it.
