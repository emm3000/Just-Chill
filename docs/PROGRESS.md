# JustChill — Progreso

> Punto de re-entrada canónico. Si retomás el proyecto después de un context
> reset, leé esto primero y después el `CLAUDE.md` del módulo que vayas a tocar.
>
> **Última actualización**: 2026-08-08 · trunk `eb8f034`
>
> Este doc se reescribió el 2026-08-08 porque quedó dos meses desactualizado y
> se perdió toda la migración KMP. El detalle histórico previo (sprints S0-S5,
> track Notion-Dark, setup de detekt, verificaciones E2E slice por slice) vive
> en el historial de git (`git log --follow docs/PROGRESS.md`) y en
> `docs/archive/`. Acá solo va lo que sigue siendo verdad.

---

## Dónde estamos ahora

App Android de finanzas personales, **local-first**, en Play Store alpha cerrada.

**No hay usuarios todavía** — ni en Android ni en iOS. La alpha cerrada no tiene
instalaciones reales más allá del device del autor. Este doc afirmaba lo contrario
("hay usuarios reales con data en el device desde `4e6de6c`"); era falso y se
corrigió el 2026-08-08. La diferencia importa: sin usuarios se puede romper data
local, rehacer la navegación y postergar compliance sin costo para nadie.

Tres tracks grandes cerrados o casi:

| Track | Estado |
|---|---|
| Producto (Fases 1-5: discovery → post-v1) | ✅ cerrado, docs en `docs/` |
| Local-first sync (slices 1-5) | slices 1-4 ✅ · slice 5 ⏳ bloqueado por tareas humanas |
| Migración KMP / Compose Multiplatform | ✅ completa y mergeada a trunk |

**Git**: `trunk` está **muy adelantado respecto de `origin/trunk` y sin pushear**.
La historia es lineal (0 merge commits). Nunca mergear sin `--ff-only`.

---

## Track: migración KMP (cerrado)

La app pasó de 3 módulos Android a 4 módulos Kotlin Multiplatform con **una sola
base Compose** para Android e iOS. 67 commits, fast-forward a trunk.

- `:shared-ui` (nuevo) tiene toda la UI, los ViewModels y el wiring de Koin.
- `:androidApp` (ex `:app`) quedó como entry point delgado; `iosApp/` es el
  proyecto Xcode que lo consume.
- iOS está **congelado, no cerrado** — ver
  [ADR 003](adr/003-freeze-ios-keep-the-compile-gate.md). Compila y corre, y auth +
  sync se validaron en runtime **una vez** contra un stack Supabase local. No está
  publicado ni en App Store ni en TestFlight, Google Sign-In es un stub, y los
  round-trips de las modales nunca se verificaron. Lo único que se sigue corriendo
  por slice es el compile gate (`compileKotlinIosSimulatorArm64`, 12.9s medidos).
  El checklist de deshielo vive en el ADR.
- Después de la migración se hizo un programa de dedup (slices A→H) que unificó
  nav host, Koin, preferencias y orquestador de sync en commonMain.

El workflow, el ledger de slices con hashes y los landmines carry-forward están
en `docs/kmp/ORCHESTRATION.md` — **es el doc vigente**. `MIGRATION_PLAN.md` y
`PHASE_3_SPEC.md` son históricos y tienen decisiones que después se revirtieron.

---

## Track: local-first sync (slice 5 en curso)

Sync multi-dispositivo **opcional** vía Supabase (LWW propio). Anónimo-local
sigue siendo el default: sign-in es opt-in desde Perfil, sin gate. Decisiones en
`docs/adr/001` y `docs/adr/002`; plan operativo en `docs/sync/PLAN.md`.

- Slice 1 ✅ schema v3: soft-delete + metadata de sync.
- Slice 2 ✅ auth opt-in (correo/contraseña, después Google) + claim-on-sign-in.
- Slice 3 ✅ motor de sync: push/pull LWW + cursor server-side.
- Slice 4 ✅ lifecycle de sync: triggers, status UI. Verificado en device.
- Slice 5 ⏳ compliance + release gate. Código hecho (parseo defensivo de enums,
  paginación con keyset, eliminación de cuenta in-app vía RPC `delete_account`,
  Crashlytics apagado en `dev`). Falta lo de abajo.

### Bloqueantes del alpha — solo los puede hacer un humano

1. Crear el proyecto Supabase cloud de prod (`supabase link` + `supabase db push`)
   y llenar los `prod.*` en `supabase.properties`.
2. Hostear `docs/PRIVACY_POLICY.md` como URL pública (Play la exige para apps con
   eliminación de cuenta).
3. Completar el Google Play Data Safety form.
4. Checklist QA multi-device: clean install, semana offline-first, sign-in tardío,
   dos devices, sign-out, y **upgrade real con APK viejo + `adb install -r`**.

Después de eso: tag + AAB.

---

## Regresiones y deuda abiertas

- 🟡 `SyncOrchestrator` no tiene trigger de reconexión: si un sync falla offline y
  vuelve la red sin escrituras nuevas, no reintenta hasta el próximo `ON_RESUME`.
  No hay pérdida de data (local-first, se auto-cura), solo latencia.
- 🟡 Deps huérfanas en `libs.versions.toml` (entre ellas `firebase-analytics`,
  declarada pero sin usar).
- 🟡 Pasada de performance de Compose pendiente: `derivedStateOf`, lambdas
  recordadas, `contentType` en `LazyColumn`.
- 🔵 Las modales de iOS (export/import/share/email) están verificadas solo a nivel
  de compilación y arranque. **Ya no cuenta como deuda**: iOS está congelado y esto
  pasó al checklist de deshielo de
  [ADR 003](adr/003-freeze-ios-keep-the-compile-gate.md).

---

## Rollback points

`pre-kmp` es el punto de retorno antes de la migración. Tags de release: `v2.2.0`,
`v2.1.0`, `v2.0.0`. Tags de sprint viejos (`pre-s0` … `post-s5`, `pre-redesign`)
siguen en el repo como marcadores históricos.

---

## Mapa de docs

- `docs/kmp/ORCHESTRATION.md` — workflow de slices + ledger. **Vigente.**
- `docs/sync/PLAN.md` — slices de sync + SQL de Supabase.
- `docs/adr/` — 001 (reversa a local-first con sync opcional), 002 (cursor de pull),
  003 (iOS congelado: se mantiene solo el compile gate, se retira el ritual).
- `docs/PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `ROADMAP_V1.md`,
  `POST_V1_PLAN.md` — definición de producto, Fases 1-5.
- `docs/DESIGN_SYSTEM.md` — tokens y componentes.
- `docs/PLAY_STORE_LISTING.md`, `docs/PRIVACY_POLICY.md` — material de publicación.
- `docs/archive/` — tracks cerrados que se conservan por el razonamiento.

---

## Cómo trabajamos acá

- Commits convencionales, en inglés, **sin Co-Authored-By**. Solo los strings de
  UI van en español.
- Historia lineal: siempre `--ff-only`, rebase si divergió, nunca merge commits.
- Nunca pushear sin confirmación explícita en ese momento.
- Trabajo de KMP / shared-ui: **un writer, review inline**. El ritual de writer +
  reviewer como sub-agentes Opus separados por slice se retiró en
  [ADR 003](adr/003-freeze-ios-keep-the-compile-gate.md) — estaba calibrado para
  usuarios en producción que no existen. El gate reforzado de
  `docs/kmp/ORCHESTRATION.md` sigue vigente, compile de iOS incluido.
- Los planes de sprint (`PLAN_S*_*.md`) son efímeros y gitignored.
