# Auth & Sync UI — Designer Blueprint

> v1.0 · Snapshot of every screen, state, and string the Supabase auth/sync work added to the app.
> Purpose: give design a complete, accurate map of what exists today, where it deviates from
> `DESIGN_SYSTEM.md`, and what is coming next — so the redesign happens once, with full context.
>
> **Stale in two places** (2026-08): §7 describes Google sign-in as upcoming — it shipped, and the
> screens now live in `:shared-ui` commonMain shared with iOS. The UI critique in §6 has not been
> re-verified since. Treat §1–§5 as a map of intent, not a current screenshot.

**TL;DR for design:** the auth/sync UI was built engineering-first. It works, but it uses bespoke
components where design-system atoms exist, has missing states (loading, focus, pending-email),
and one misleading pattern (error snackbars show a green success check). Section 6 is the
prioritized worklist. Section 7 is the upcoming Google button you should design for *now*.

---

## 1 · Where auth/sync lives in the app

```
Bottom bar → Perfil (ProfileScreen)
              ├── CUENTA section ──────────────┐
              │     signed-out: "Iniciar sesión" row → AuthScreen
              │     signed-in:  email + sync status row
              │                 "Cerrar sesión" row
              │                 "Eliminar cuenta" row → DeleteAccountDialog
              ├── RESPALDO section (export / import → confirm dialog)
              └── APP section → PrivacyPolicyScreen (mentions account/sync)

Global, any screen:
  • Snackbar (bottom): sync failed, session expired, auth results
```

Auth is **opt-in and secondary**: the app is fully usable with no account (local-first).
The account's only promise is multi-device sync. Design should keep that hierarchy —
auth never interrupts, never blocks, never appears outside Perfil.

---

## 2 · Screen specs (current state)

### 2.1 AuthScreen — sign in / sign up

Single screen, two modes toggled in place. No separate registration screen.

| Element (top → bottom) | Spec today |
|---|---|
| Top bar | `JcTopBar`, title "Tu cuenta", back arrow left |
| Email field | Custom `AuthFieldInput`: label "Correo", placeholder "hola@ejemplo.com", 1dp underline `border` |
| Password field | Same component: label "Contraseña", placeholder "••••••••", masked, **no show/hide toggle** |
| Mode toggle | Centered tappable text, `bodyM` in `accent` |
| CTA | `StickyCTA`, accent tone, pinned to bottom |

**Modes and states:**

| State | What the user sees |
|---|---|
| SignIn (default) | CTA "Iniciar sesión" · toggle "¿No tienes cuenta? Créala" |
| SignUp | CTA "Crear cuenta" · toggle "¿Ya tienes cuenta? Inicia sesión" |
| Loading | CTA dims (disabled). **No spinner, no other feedback** |
| Sign-in success | Navigates back to Perfil silently |
| Sign-up needs email confirmation | Snackbar "Te mandamos un correo — confírmalo y vuelve a iniciar sesión." — then the screen stays exactly as it was, fields still filled. **No dedicated "check your inbox" state** |
| Error | Snackbar with error message (see §4); screen unchanged |

**Field anatomy (current, all hardcoded — not tokens):** label 12sp W500 `textTertiary`
(letterSpacing 0.4) · input 16sp W500 `textPrimary` · placeholder 16sp W400 `textTertiary` ·
cursor `accent` · underline 1dp `border`, **never changes on focus** (`borderFocus` token
exists but is unused) · vertical padding 8dp (design system §7.2 says 12dp).

### 2.2 ProfileScreen — CUENTA section

| Session state | Rows shown |
|---|---|
| Signed out (and while session is still resolving) | "Iniciar sesión" / meta "Sincroniza tus datos entre dispositivos" / icon `Shield` |
| Signed in | 1. Account row: email (or "Tu cuenta") + sync status meta + `AccountCircle` icon; trailing = chevron, or 16dp spinner while syncing. **Row tap does nothing today despite the chevron.** 2. "Cerrar sesión" / meta "Tus datos siguen en este teléfono" / icon `Shield` (same icon as Privacidad row). 3. "Eliminar cuenta" / meta "Borra tu cuenta y tus datos en la nube" → "Eliminando…" while in flight |

**Sync status meta (account row), one of:**

| Condition | Text |
|---|---|
| Sync in flight | "Sincronizando…" |
| Has synced before | "Última sincronización: 5 jun, 14:32" |
| Never synced yet | "Sincronización activa" |

Debug builds add a DEBUG section with a manual "Sincronizar ahora" row — not shipped to users.

### 2.3 DeleteAccountDialog

Custom dialog (not Material): `surface2` bg, 24dp radius (hardcoded; system says 12dp for
dialogs), 40dp icon tile (`negMuted` bg, `danger` trash icon), title "¿Eliminar tu cuenta?",
body "Se eliminará tu cuenta y todos tus datos en la nube. Tus datos seguirán disponibles en
este teléfono.", two bespoke buttons: "Cancelar" (surface1/border) · "Eliminar" (solid `danger` —
design system §7.1 specifies destructive buttons as *transparent bg + danger text/border* instead).

### 2.4 Import confirmation dialog (RESPALDO)

Plain Material3 `AlertDialog` — visually off-theme vs the custom dialog above. Title
"¿Reemplazar tu data?", body "Esto va a borrar todo lo que tengas hoy y poner lo del archivo.",
actions "Reemplazar todo" (danger text) / "Cancelar". Two different dialog languages now coexist.

### 2.5 PrivacyPolicyScreen

Title "Tu privacidad" + 6 body paragraphs (`bodyL`). Exit only via a bottom "Volver" button —
a raw Material3 Button (56dp, accent), not `StickyCTA`; no top bar, no back arrow.
Copy already explains the optional-account model and in-app account deletion.

### 2.6 Snackbar (global)

`EmmSnackbar`: `surface2` bg, 1dp border, 14dp radius, 13sp text. Leading icon is a green
check in a `posMuted` circle — **always, including for errors**. "Tu sesión expiró…" renders
with a success check. Needs an error variant (and possibly neutral/info).

---

## 3 · Sync feedback model

Sync runs automatically (app foreground, after sign-in, debounced after local edits). There is
**no global sync indicator** — all feedback lives in the Perfil account row, plus two global
snackbars:

| Event | Surface | Message |
|---|---|---|
| Sync running | Perfil account row | Spinner + "Sincronizando…" |
| Manual sync failed | Snackbar | "No se pudo sincronizar. {error}" |
| Session expired mid-sync | Snackbar (user is signed out automatically) | "Tu sesión expiró. Inicia sesión nuevamente." |

Background (automatic) sync failures are intentionally silent. Design question for v-next:
does the account row need an error state ("No se pudo sincronizar · reintentar")?

---

## 4 · Copy inventory (all user-facing strings)

Tone reference: voseo-free, Peru-casual ("capaz", "tu celu", "tu plata"). Keep it.

**Auth:** "Tu cuenta" · "Correo" · "hola@ejemplo.com" · "Contraseña" · "Iniciar sesión" ·
"Crear cuenta" · "¿No tienes cuenta? Créala" · "¿Ya tienes cuenta? Inicia sesión" ·
"Te mandamos un correo — confírmalo y vuelve a iniciar sesión."

**Perfil/CUENTA:** "Iniciar sesión" · "Sincroniza tus datos entre dispositivos" · "Tu cuenta" ·
"Sincronizando…" · "Última sincronización: {d MMM, HH:mm}" · "Sincronización activa" ·
"Cerrar sesión" · "Tus datos siguen en este teléfono" · "Eliminar cuenta" ·
"Borra tu cuenta y tus datos en la nube" · "Eliminando…"

**Dialogs:** "¿Eliminar tu cuenta?" · "Se eliminará tu cuenta y todos tus datos en la nube.
Tus datos seguirán disponibles en este teléfono." · "Cancelar" · "Eliminar" ·
"¿Reemplazar tu data?" · "Esto va a borrar todo lo que tengas hoy y poner lo del archivo." ·
"Reemplazar todo"

**Result snackbars:** "Sesión cerrada. Tus datos siguen en este teléfono." · "Cuenta eliminada.
Tus datos siguen en este teléfono." · "Listo, tu data está guardada." · "Listo — {N} movimientos
importados." · "No pude exportar — capaz no hay espacio en tu celu?" · "No pude importar el
archivo — capaz está dañado." · "No se pudo sincronizar. {error}" · "Tu sesión expiró. Inicia
sesión nuevamente."

**Error messages (mapped from failures):** "No encontré eso" · "Algo no cuadra con los datos" ·
"Hubo un problema guardando tu data" · "Credenciales incorrectas o sesión expirada" ·
"Sin conexión — revisa tu internet" · "Algo se rompió — capaz reinicia la app?"

---

## 5 · Design-system deviations (engineering-confirmed)

Tokens/atoms exist for almost everything below — these screens just predate or bypass them.

| # | Where | Deviation |
|---|---|---|
| 1 | AuthScreen fields | Bespoke `AuthFieldInput`; typography hardcoded instead of `labelM`/`bodyL`; 8dp padding vs spec 12dp; no focus state (`borderFocus` unused) |
| 2 | DeleteAccountDialog | 24dp radius vs spec 12dp; bespoke buttons; solid-danger confirm vs spec transparent-destructive |
| 3 | Import dialog | Raw Material3 `AlertDialog`, doesn't match the custom dialog pattern |
| 4 | PrivacyPolicyScreen | Raw Material3 Button instead of `StickyCTA`; no top bar |
| 5 | ProfileScreen | One-off `HairlineDivider` (a `Hairline` atom exists); meta text 13sp (no such token); icon tile radius hardcoded |
| 6 | EmmSnackbar | Single variant — success check shown for errors too |
| 7 | DESIGN_SYSTEM.md §13 | Still says "Not in this roadmap: any Auth / Login / Register screen" — outdated, needs an Auth section |

---

## 6 · UX debt — prioritized worklist for design

**P0 — misleading or blocking feedback**
1. Error snackbar shows a success check → design an error (and info?) snackbar variant.
2. AuthScreen loading: button only dims; no progress feedback → loading state for the CTA.
3. Sign-up email-confirmation: snackbar-only, screen stays as-is → design a dedicated
   "Revisa tu correo" state/screen (what it shows, how the user gets back to sign-in).

**P1 — broken affordances**
4. Signed-in account row shows a chevron but does nothing → either design an account detail
   destination or drop the chevron.
5. Password field has no show/hide toggle.
6. Field focus state missing (underline never reacts).
7. Export/Import rows give zero feedback while running and stay tappable.

**P2 — consistency**
8. Icon semantics: "Iniciar sesión" and "Cerrar sesión" both use the Shield icon (also used
   by Privacidad). Pick distinct icons.
9. Unify the two dialog styles (custom vs Material3) into one pattern.
10. Session "resolving" moment renders as signed-out for a flash → decide: skeleton row,
    or accept the flash.
11. Token cleanup per §5 (engineering task once design confirms intended values).

---

## 7 · Coming next: Google sign-in (design for this now)

Google OAuth is the next auth feature. Email/password stays; Google is added alongside.
Impact on AuthScreen:

- A **"Continuar con Google" button** — must follow
  [Google's sign-in branding guidelines](https://developers.google.com/identity/branding-guidelines)
  (official G logo, approved button shapes; dark-theme variant fits our monochrome UI well).
  Decide placement: above the email form (recommended — it will be the majority path on
  Android) or below with an "o" divider.
- Google flow has **no password, no email confirmation** — the "Te mandamos un correo" state
  only applies to the email path.
- Account row in Perfil: a Google session shows the Google account email; consider showing
  the provider ("Google") in the meta or detail view.
- Error states: user cancels the Google sheet (silent, no snackbar) vs Google flow fails
  (error snackbar).

Designing AuthScreen with the Google button from the start avoids a second redesign in weeks.

---

## 8 · Reference

- Tokens & components: `docs/DESIGN_SYSTEM.md` (colors §3, type §4, buttons §7.1,
  inputs §7.2, dialogs §7.7).
- Product framing: account is optional, local-first is the manifesto
  (`docs/PRODUCT_DISCOVERY.md`).
- Source of truth for behavior described here (post-KMP paths):
  `shared-ui/src/commonMain/kotlin/com/emm/justchill/hh/auth/`, `hh/profile/`,
  `hh/shared/AppNavHost.kt`, `core/sync/SyncOrchestrator.kt`. The Android-only Google
  credential plumbing is in `androidApp/src/main/kotlin/com/emm/justchill/hh/auth/`.
