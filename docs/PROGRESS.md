# JustChill — Progreso

> Leé esto primero, y después el `CLAUDE.md` del módulo que vayas a tocar. Acá no se anota
> hash de commit, conteo de commits ni número de línea: el commit que los escribe ya los
> deja viejos. Acá tampoco se anota trabajo: si hay compromiso, es un ticket en `docs/work/`.

## Dónde estamos ahora

- App Android de finanzas personales, local-first, en Play Store alpha cerrada.
- Toda migración preserva la data y se prueba con `:data:connectedAndroidDeviceTest` antes de
  `trunk`. El porqué — la instalación del autor tiene data real — lo dice el header de `CLAUDE.md`.
- El trabajo comprometido no se lista acá: vive en `docs/work/`. El índice es `eza docs/work/backlog`.

## Cómo se verifica el estado

```bash
git rev-list --count origin/trunk..trunk            # commits sin pushear
git rev-list --count --merges origin/trunk..trunk   # debe dar 0: la historia es lineal
gh api repos/emm3000/Just-Chill/branches/trunk/protection \
  --jq '{checks: .required_status_checks.contexts, admins: .enforce_admins.enabled,
         linear: .required_linear_history.enabled, force: .allow_force_pushes.enabled}'
eza docs/work/backlog                                # el trabajo con ticket
```

GitHub ya impone la historia lineal en `trunk` y le cerró el force-push, así que el merge commit
y la reescritura de historia ya pusheada no dependen de que te acuerdes. Lo que sigue dependiendo
de vos: `enforce_admins: false` es deliberado — pusheás directo a `trunk`, y en ese camino
`quality-gate` **no corre**. La única red ahí es el hook de pre-push, que corre
`qualityGate` local. Si alguna vez querés que el CI sea la red, hay que prender `enforce_admins`,
y desde ese momento cada cambio necesita branch y PR. Toda la protección se cambia con un `PUT`
del objeto completo, nunca un PATCH.

## Ideas sin ticket

Ideas, no compromisos: ninguna tiene ticket y ninguna entra sin pasar el criterio de aceptación de
`docs/PRODUCT_REQUIREMENTS.md` §3. Abrir ticket en `docs/work/` recién cuando se tome una.

- **Tema claro.** Hoy la app es solo dark: nada en `:ui-android` lee `isSystemInDarkTheme`.
- **Export CSV** para abrir en Excel. Hoy el único formato de salida es el JSON del backup.
- **Recordatorio gentil opt-in.** Roza W-08 (`PRODUCT_REQUIREMENTS.md` §1): si entra, es opt-in y
  no diario, o hay que renegociar la fila.
- **Widget Android del saldo del mes**, para verlo sin abrir la app.
- **Búsqueda avanzada** por rango de monto. Las fechas y la categoría ya filtran en SeeTransactions.
- **Onboarding con data de ejemplo.** Hoy el primer launch es solo el manifiesto (`ManifestoScreen`).
- **Harness de test de Compose.** `:ui-android` solo tiene `androidHostTest`: sin Robolectric ni
  `ui-test-junit4`, ningún test puede pinear un bug que vive en un `when` de composición. Uno así
  —la sección de pendientes de `SeeTransactionsScreen` desaparecía en mes vacío— pasó el gate entero
  y lo atrapó un reviewer, no la suite. La red hoy son los `@Preview`, y solo miran quienes los abren.

## Rollback points

`pre-kmp` es el punto de retorno antes de la migración KMP. Tags de release: `v2.4.0`, `v2.3.0`,
`v2.2.0`, `v2.1.0`, `v2.0.0`, `v1.6.0`.
