# JustChill — Progreso

> Leé esto primero, y después el `CLAUDE.md` del módulo que vayas a tocar. Acá no se anota
> hash de commit, conteo de commits ni número de línea: el commit que los escribe ya los
> deja viejos. Ítem de 1-2 líneas más un puntero; el razonamiento va en el ticket o en git.

## Dónde estamos ahora

- App Android de finanzas personales, local-first, en Play Store alpha cerrada.
- Toda migración preserva la data y se prueba con `:data:connectedAndroidDeviceTest` antes de
  `trunk`. El porqué — la instalación del autor tiene data real — lo dice el header de `CLAUDE.md`.
- El trabajo de backup/sync no se lista acá: vive en `docs/work/`. El índice es `eza docs/work/backlog`.

## Cómo se verifica el estado

```bash
git rev-list --count origin/trunk..trunk            # commits sin pushear
git rev-list --count --merges origin/trunk..trunk   # debe dar 0: la historia es lineal
gh api repos/emm3000/Just-Chill/branches/trunk/protection --jq '.required_status_checks.contexts'
eza docs/work/backlog                                # el trabajo con ticket
```

Nunca mergear sin `--ff-only`. `trunk` exige PR + status checks (`quality-gate`, `ios-compile`),
pero admin los bypassea: en un push directo esos checks **no corrieron**.

## Trabajo abierto

### Fechas

- [ ] Migrar la columna de Supabase de `date bigint` a `text`, UTC-5 (no `AT TIME ZONE 'UTC'`).
      SQL en verde, 7/7 vectores; falta secuenciarlo sobre data real. `docs/archive/sync/AUDIT.md` §5.
- [ ] Detector de drift en CI con el md5 normalizado del schema del server, y mover el guard de
      `relreplident` después del `continue` de idempotencia (`supabase/migrations/`).

### Infraestructura

- [ ] Subir el wrapper de Gradle 9.7.0 → 9.7.1. Único warning de `:androidApp:lintProdRelease`; queda
      visible porque dependabot parsea `libs.versions.toml`, no `gradle-wrapper.properties`.
- [ ] Decidir si la protección de `trunk` alcanza a admin (`enforce_admins: false` hoy).
- [ ] `required_linear_history: false` y `allow_force_pushes: true`, ambas a conciencia. Cambiarlas
      exige un `PUT` del objeto de protección completo, no un PATCH.

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

## Rollback points

`pre-kmp` es el punto de retorno antes de la migración KMP. Tags de release: `v2.4.0`, `v2.3.0`,
`v2.2.0`, `v2.1.0`, `v2.0.0`, `v1.6.0`.
