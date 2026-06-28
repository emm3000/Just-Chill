# JustChill — Progreso v1

> Estado del proyecto a fecha del último update. Punto de re-entrada
> para retomar después de cerrar/limpiar el contexto.
>
> **Última actualización**: 2026-06-10 (local-first-sync: slices 1-4
> commitados y verificados. Slice 5 en curso: 5 commits landed, ver
> "Slice 5" abajo. Próximo: Play Data Safety form + prod Supabase
> (humano) → tag + AAB).

---

## Track activo — local-first-sync (2026-06-04 → )

Reversa del posicionamiento local-only: la app pasa a ser **local-first
con sync multi-dispositivo OPCIONAL** vía Supabase (LWW propio, sin
PowerSync). Anónimo-local sigue siendo el estado default — sign-in es
opt-in desde Perfil, sin gate. Decisión documentada en `docs/adr/001`
(+ `docs/adr/002` para el cursor de pull server-side). PRD amendado
(W-02/W-03/W-11 ahora opt-in, con nota fechada).

**Decisiones en `docs/adr/001` + `docs/adr/002`.** El plan operativo
(slices 2-5, tasks, SQL de Supabase re-derivado) vive en
`docs/sync/PLAN.md` — re-derivado 2026-06-09 desde los ADRs + código
de trunk + git history, porque el design original quedó en engram cuando
estuvo desactivado (engram + SDD se **re-habilitaron** el 2026-06-10 en
commit `55fcab1`).

**Estado**: slices 1-4 de 5 ✅ commitados y verificados en device.
Slice 5 ⏳ en curso: 5 commits landed (parseo defensivo de enums,
paginación de pull, reescritura de privacidad + baja de Analytics,
fixes de auditoría, eliminación de cuenta in-app). Ver "Slice 5" abajo.
Próximo: Play Data Safety form + prod Supabase (humano) → tag + AAB.

Slice 1 ✅ (`59b8adf`):
- Migración 2.sqm (schema v3): `userId`/`deletedAt`/`syncState` + índices en las 4 tablas.
- Soft-delete con tombstones; todas las lecturas filtran `deletedAt IS NULL`.
- Integridad referencial movida a `DeleteAccountUseCase`/`DeleteCategoryUseCase` (los FK no disparan en soft-delete).
- Tests: domain verdes, 5/5 instrumentados, detekt limpio. Pasó review adversarial (4 fixes en `59b8adf`).

Slice 2 ✅ (auth opt-in, `ce5180f`→`700d28b`, 8 commits):
- `ce5180f` build: deps Supabase + BuildConfig por flavor desde `supabase.properties`.
- `501ed4f` domain: `AuthRepository`, `SessionStatus`, use cases `SignIn/Up/Out`, `ObserveSession`, `ClaimLocalData`.
- `668a415` data: `DefaultAuthRepository` sobre auth-kt + claim atómico local.
- `0e42d3b` data: hardening del threading del claim + error mapping.
- `17ecfdb` domain: claim via session observer (no inline).
- `700d28b` app: sección "Cuenta" en Perfil (sign-in opt-in) + `AuthScreen` MVI, sin gate.
- Entorno dev migrado a **stack Supabase local** (`supabase start`, Docker) — ver `docs/sync/PLAN.md §Environments`. Prod cloud sigue pendiente (humano), solo bloquea slice 5.
- Tests verdes: `./gradlew test` BUILD SUCCESSFUL. Domain (`SignIn/Up/Out`, `ObserveSession`, `ClaimLocalData*`) + data mappers (`AuthExceptionMapper`, `SessionStatusMapper`, `DefaultClaimLocalDataRepository`).
- **Blocker encontrado + arreglado durante verificación en device (commit `b300499`)**:
  S0 había quitado el permiso `INTERNET` + cleartext; slice 2 restauró las
  deps de Supabase pero no el acceso a red, así que el auth fallaba en
  silencio (ni user creado ni logs). Fix: `INTERNET` en el manifest main +
  `network_security_config` solo-dev permitiendo cleartext a `10.0.2.2`/
  localhost (prod queda https-only). **Verificado E2E en Medium Phone contra
  el stack local**: sign-up crea user en Supabase, sesión sobrevive
  process-death, claim setea `userId`+`syncState=Pending` en las 4 tablas,
  sign-out conserva la data local. Usuario de prueba: `test2@justchill.dev` /
  `secret123`.

---

### Slice 3 — cerrado (2026-06-10)

Sync engine core: push/pull LWW + cursor global para las 4 tablas,
trigger manual debug "Sincronizar ahora" en Perfil. Arquitectura:

- `:domain/sync/`: `ConflictResolver` (LWW puro, tie→remote, tombstone-aware sin special-casing), `SyncRepository`, `SyncDataUseCase` (mutex compartido — Koin `single`), `SyncCursorStore` (port; impl en `:app`).
- 4 `.sq`: `selectPending`, `markSynced` (guardada por `updatedAt`), `findForSync` (NO filtra `deletedAt` — LWW debe ver tombstones), `insertOrIgnoreFromRemote` + `updateFromRemote` (upsert en 2 statements — SQLDelight 2.3.2 NO parsea `ON CONFLICT DO UPDATE`; `OR REPLACE` dispararía FKs), `markPendingForResync`.
- `:data/sync/`: `BaseTableSync<DTO>` (el algoritmo push/pull vive UNA sola vez; subclases solo aportan queries generadas + llamadas postgrest reified), DTOs `@Serializable` snake_case con interface `SyncRowDto`, `SyncCursorUtils` (parse `+00:00` de PostgREST, overlap 10s, canónico `Z`), `DefaultSyncRepository` (push 4 → pull FK-safe, cursor global con hold si hubo skips).
- `:app`: `AppPreferences.lastPulledAt_<userId>`, `SyncModule` (qualifiers nombrados), fila debug en Perfil.

**Hardening (2 rondas de judgment-day, doble juez ciego, ambas APPROVED)**:
ronda 1 corrigió 6 bugs de la implementación inicial (FK/REPLACE, `Instant.parse`
vs `+00:00`, cursor no monótono, race de `markSynced`, sesión `Initializing`,
baseline detekt); ronda 2 corrigió 6 más: tests de `:app` rotos, mutex roto por
scope `factory`, scoping `user_id` en pull/push (defense-in-depth sobre RLS),
**`KeepLocal` re-marca `Pending`** (sin eso: push ciego = last-pusher-wins y
divergencia permanente), skip por fila de huérfanos FK con cursor-hold,
`withTimeout(10s)` en resolución de sesión.

**Tests** (PLAN task 7 cumplido): 49 nuevos — `SyncCursorUtilsTest` (11, pin
del bug `+00:00`), `DefaultSyncRepositoryTest` (16: orden FK-safe, avance de
cursor como `Instant`, cursor-hold con skips, timeout→`NetworkUnavailable`),
`SyncQueriesTest` (19 contra SQLite real JVM: upsert 2-statements no dispara
FK de hijos, guard de `markSynced`, tombstones visibles a `findForSync`),
mutex serialization en `SyncDataUseCaseTest`, + `SyncFkExceptionTest`
instrumentado (2: pin de que `SQLiteConstraintException` ES el tipo que lanza
el driver Android en huérfanos FK y que cacharlo dentro de la tx NO la
rollbackea — verificado también contra sqlite3 CLI y xerial JDBC).

**Verificación E2E en 2 devices (2026-06-10) — TODO PASÓ**:
- Push A→server: 26 filas (1 account, 23 categories, 2 tx) `Pending`→`Synced`, visibles en Postgres local.
- 2do sync en A: sin crash (el path del cursor), cursor persistido `Z` canónico per-user.
- Pull a device B limpio: 26 filas convergen.
- Tombstone: delete de tx en A propaga a B (`deletedAt` seteado, fuera de la UI).
- LWW real: misma tx editada en ambos (B→600 primero, A→700 después), sync B→A→B: **ambos devices + server convergen en 700** (gana el write más nuevo).

Residuales conocidos (no bloqueantes, candidatos slice 4):
- Cursor congelado si el server tuviera una fila huérfana PERMANENTE (requiere inconsistencia referencial server-side; el race transitorio se auto-cura al siguiente ciclo). Candidato: retry cap / poison-row defer.
- Clear de server tombstone en "resurrect" del mismo PK — no alcanzable (no hay undelete), documentado a propósito.
- N+1 `findForSync` por fila en pull — irrelevante a escala personal; ahora viviría en UN solo lugar (`BaseTableSync`) si se optimiza.

**Bug PRE-existente encontrado de paso (NO sync)**: el editor de
transacciones divide el monto por 1000 al CARGAR (tx de S/ 25.00 abre
como "2.50"); guardar convierte bien (600.00 → 60000 centavos). Display-only
al load, pero guardar sin re-tipear achicaría el monto 10×. Tech debt:
unificar el formatter de plata entre lista y editor.

**Follow-up tracked antes de slice 3**: test instrumentado E2E de los
delete use cases contra SQLite real (hoy solo fakes MockK).

---

### Slice 4 — sync lifecycle: cerrado (2026-06-10)

Slice 4 (triggers automáticos: on-resume, write debounced 3s,
sync-on-sign-in + UI "Última sincronización" en Perfil) implementado,
commitado y **verificado en device** (medium_phone, stack Supabase local).
La verificación destapó un bug **crítico pre-existente** que rompía la
feature estrella del slice. Fix aplicado + re-verificado antes de commitear.

**Síntoma**: una transacción creada estando YA logueado no sincroniza
— ni con el trigger debounced, ni con sync manual, ni en el siguiente
launch. Solo sincroniza tras **reiniciar la app**.

**Causa raíz**: el `insert:` de las 4 tablas nunca setea `userId`
(queda NULL); lo único que lo estampa es `claimAll`. Y
`ClaimLocalDataOnAuthenticationUseCase` corría `claimAll` solo en
transiciones `observeSession().mapNotNull{userId}.distinctUntilChanged()`
— o sea UNA vez por login. Estando ya autenticado, una fila nueva
(userId NULL) no genera nuevo emission de sesión → `distinctUntilChanged`
la suprime → nunca se reclama. Como `selectPending` y `countPending`
filtran `userId IS NOT NULL`, la fila es invisible tanto al push como al
trigger debounced de slice 4. **Doble impacto**: (1) el trigger de
escritura nunca dispara; (2) la fila nunca pushea. Solo el cold start la
salva (flow nuevo → primer emission pasa el distinct → `claimAll` la
barre). El E2E de slice 3 no lo vio porque todas sus filas se creaban
anónimas y se reclamaban en sign-in — nadie creó una fila DESPUÉS de
loguearse.

**Fix aplicado (claim reactivo)**: `ClaimLocalDataOnAuthenticationUseCase`
ahora reclama cuando hay sesión `Authenticated` **Y** hay filas sin
dueño, no solo en transición de userId. Nuevo
`ClaimLocalDataRepository.observeUnclaimedCount(): Flow<Long>` (combine
de 4 `countUnclaimed: SELECT COUNT(*) WHERE userId IS NULL`),
`flatMapLatest` sobre sesión, `.filter { it > 0 }`, **sin
`distinctUntilChanged`** (era justo lo que causaba el bug). `claimAll`
es idempotente + atómico, así que reclamos redundantes son seguros; tras
reclamar, count→0 (filtrado), sin loop. Hace que el código honre su
propio KDoc ("rows created while offline get claimed"). 202 tests domain
+ testDevDebug + detekt verdes.

**Verificado en device post-fix (medium_phone)**: crear S/678.90
logueado → en server en ~3s sin botón ni reinicio; on-resume trae
cambios server en ~2s; sync-on-sign-in trae 4 tx / 1 cuenta / 23 cat en
~2s; cero crashes (el fix `flowOn(Main.immediate)` de `ProcessResumeEvents`
aguanta); 0 filas huérfanas.

**Tech debt nuevo (NO arreglado — candidato slice 5)**:
`TransactionMappers.toDomain` hace `TransactionType.valueOf(type)` sin
fallback. Una fila remota con `type` inesperado (rename de enum, drift
de schema, data externa) lanza `IllegalArgumentException` no manejada en
el Flow de la lista (collect en Main) → la app entra en **crash-loop al
arrancar** (la lista es el tab inicial). Imposible en operación normal
hoy (la app controla todas las escrituras vía el enum), pero el sync
vuelve la data remota un input real. Mismo riesgo en `CategoryType.valueOf`
y cualquier `valueOf`-sobre-remoto en los mappers. Fix: parseo defensivo
(unknown → skip fila / default) en el path remoto→dominio. Va junto con
la paginación de pull en slice 5. (Descubierto porque una fila de prueba
inyectada por REST usó `"INCOME"` en vez de `"Income"` — el server
guarda lo que se le pushea y el `valueOf` del cliente es case-sensitive
y sin guarda.)

Notas laterales:
- Branch `feat/income-widget` (`eafc8ec`, sin push): widget Glance de
  ingresos por fuente del mes. Funcional, pendiente de decidir si entra.
- El repo en GitHub fue renombrado `android-retrofit` → `Just-Chill`;
  el remote `origin` local sigue apuntando a la URL vieja (funciona por
  redirect de GitHub).
- Engram y SDD estuvieron desactivados un tiempo, pero se
  **re-habilitaron el 2026-06-10** (commit `55fcab1`, removió la nota del
  `CLAUDE.md` raíz). El estado canónico sigue viviendo en `docs/` + git
  (committeable, compartible), y engram queda como memoria persistente
  cross-sesión en paralelo.

---

### Slice 5 — compliance + release gate: en curso (2026-06-10)

5 commits en trunk:

- `34cca18` — parseo defensivo de enums en mappers remotos: valores
  desconocidos hacen skip de fila en vez de crash (`IllegalArgumentException`
  no manejada en el Flow de lista = crash-loop al arrancar). Cubre
  `TransactionType`, `CategoryType` y cualquier `valueOf`-sobre-remoto.
- `297fad3` — paginación de pull con composite keyset `(server_updated_at, pk)`:
  elimina el truncamiento silencioso del pull (límite default de 1000
  filas de PostgREST) en datasets remotos grandes.
- `50ff8d4` — reescritura de `docs/PRIVACY_POLICY.md` + `PrivacyPolicyScreen`
  para data que opcionalmente sale del device; dependencia
  firebase-analytics removida (Crashlytics se mantiene en `prod`).
- `65da609` — fixes de auditoría: Crashlytics deshabilitado en `dev`;
  guard de stop-limpio en la paginación de pull.
- `359b9ce` — eliminación de cuenta in-app: RPC `delete_account` (security
  definer server-side); local data preservada vía `unclaimAll`
  (userId→NULL, syncState→Pending, tombstones incluidos); prefs de cursor
  per-user limpiados; E2E verificado en emulator (ver registro abajo).

**Verificación E2E — eliminación de cuenta (2026-06-10)**

Entorno: emulator-5554 (Medium Phone), stack Supabase local
(`supabase start`), migración aplicada vía `supabase migration up`.
Usuario de prueba pre-existente: `test2@justchill.dev` (28 filas remotas:
1 account, 23 categorías, 4 tx). Nuevo post-delete: `test3@justchill.dev`.

Verificado:
- Flujo de eliminación remueve el auth user + todas las 28 filas remotas
  del servidor.
- Filas locales revierten a `userId = NULL` / `syncState = 'Pending'`
  (incluyendo tombstones que quedan marcados `Pending` para propagarse
  si el usuario vuelve a loguearse).
- Prefs de cursor per-user removidos (el siguiente sign-in arranca con
  full re-pull).
- App completamente usable firmado-out tras eliminar; sin crash-loop, sin
  filas huérfanas.
- "Cancelar" en el diálogo de confirmación no toca nada — datos y sesión
  intactos.
- Sign-up con `test3@justchill.dev` (nueva cuenta) re-clama las 28 filas
  locales vía `ClaimLocalDataOnAuthenticationUseCase` y las pushea bajo
  el nuevo `userId` — claim-on-sign-in confirmado en ambas direcciones.
- Entorno limpio post-test: `test2@justchill.dev` eliminado; `test3@justchill.dev`
  ahora es dueño de la data.

**Pendiente (código)**:
- ~~Tests instrumentados E2E de delete use cases contra SQLite real~~
  ✅ hecho — `DeleteUseCasesE2ETest.kt`, 9 tests contra SQLite real
  (16/16 connected verdes en emulator). Gotcha: `kotlin.assert()` es
  no-op en ART; usar siempre `kotlin.test.assertTrue`.
- Checklist QA multi-device (clean install, semana offline-first,
  sign-in tardío, dos devices, sign-out, **upgrade real con APK viejo
  + `adb install -r`**).
- Tag + AAB.

**Tareas humanas (no las puede hacer el agente)**:
- Crear proyecto Supabase cloud prod: `supabase link --project-ref <ref>`
  + `supabase db push` → llenar `prod.*` en `supabase.properties`.
- Hostear `docs/PRIVACY_POLICY.md` como Gist público (Play exige URL de
  política de eliminación para apps con account deletion).
- Completar Google Play Data Safety form (obligación legal per ADR 001).

---

## Track previo — Reporte v2 (2026-05-21)

Llegó un handoff de diseñador externo con dos pantallas: `Reporte ·
Mes` (polish encima del SR-5 existente) y `Reporte · Tendencias`
(nueva). Cruce contra DS encontró 5 divergencias resueltas en el
DS antes de codear:

- §3.4: agregada excepción "eyebrow all-caps permitido" (§7.13 docs el rol).
- §7.10: agregada variante card-around-bars (grouped variant) cuando
  hay header + footer en la misma sección.
- §7.13 (nueva): vertical bar chart 6 meses con `cat.sage` (entró)
  + `cat.terracotta` (salió). **NO** verde/rojo — §1.4 preservado
  (income/expense por signo, no por color; los earth tones del set
  `cat.*` son aceptables porque están en la paleta curada).
- §7.14 (nueva): comparison pill + section footer (X movimientos ·
  Promedio S/ Y).
- §7.15: empty states renumerado (era §7.12).

US-22 añadida al PRD (Should, no Must) — *Tendencias 6 meses*.
Caveat honesto en el AC: roza la línea del manifesto ("tu plata no
necesita un dashboard"), si dogfooding muestra que no se usa se
difiere a v2 sin culpa.

Scope concreto pendiente:
- **Mes polish**: total con decimales tenues, pill verde con delta
  absoluto + %, card alrededor de "POR CATEGORÍA" con header
  (`Eyebrow + counter`) + footer (`X movimientos · Promedio`),
  botón share en topbar + sticky "Compartir reporte" al fondo.
- **Tendencias**: tab pill `Mes/Tendencias` arriba, savings rate
  6m + delta vs 6m previos, bar chart vertical 6 meses
  (componente custom Canvas, NO lib), top 3 categorías de gasto
  con "Top en X de 6 meses".
- Share intent: `Intent.ACTION_SEND` text/plain con resumen del
  tab actual.

---

## ⚠️ Rollback disponible — Notion-style typography sweep (2026-05-20)

Se aplicó una barrida tipográfica Notion-style sobre 17 archivos del módulo `:app`:
sube body de 13-14sp → 15sp con `letterSpacing = -0.15sp`, switch de
IBM Plex Mono → Inter+tnum en montos inline, fix crítico de `fontSize = 9.sp`
en `TransactionFormControls.kt`, y construcción de jerarquía por **color/peso**
en vez de por tamaño (filosofía Notion).

El cambio compiló y pasó detekt, pero **no fue validado en dispositivos reales
de gama baja** al momento del commit. Si aparecen regresiones visuales
(textos cortados, overflow en pantallas <360dp, descuadres en filas):

```bash
git log --oneline | grep "typography sweep"   # encontrar el SHA
git revert <sha>                              # revertir el sweep entero
```

El commit es atómico y autocontenido — `git revert` lo deshace sin tocar
el resto del trabajo. El fix de layout en `ProfileScreen.ProfileRow`
(stacked label+meta) va en commit separado y **no debe revertirse**
aunque se revierta el sweep — es un fix de bug independiente que aplica
con cualquier tamaño de letra.

---

## TL;DR — dónde estamos ahora

- **Track activo — local-first-sync**: slices 1-4/5 commitados y
  verificados en device. Slice 5 ⏳ en curso — 5 commits landed (parseo
  defensivo, paginación de pull, privacidad, auditoría, eliminación de
  cuenta). Pendiente: Play Data Safety form + prod Supabase (humano),
  luego QA checklist + tag + AAB. Decisiones en `docs/adr/`.
  OJO: la decisión "100% local, sin login" de Fases 1-5 fue **reversada
  formalmente** vía ADR 001 — ahora es local-first con sync opcional.
- **Proceso de definición**: ✅ Fases 1-5 firmadas y versionadas.
- **Ejecución**: Sprints 0-5 + mini-S6.5 cerrados. AAB `2.0.0-alpha`
  tagueado (pre-redesign) pero **aún no subido a Play Console**.
- **Post-alpha track 1 — Notion-Dark visual refactor** (cerrado):
  rediseño completo de la UI sobre el handoff de Claude Design.
  SR-1..SR-10 commiteados en `trunk` (foundations + Home + Add/Edit
  tx + Reporte + Ver + Cuentas + Categorías + Perfil + Manifesto +
  Calendar). La UI actual es **mucho mejor que la del AAB tagueado**
  — cualquier screenshot para Play Store debería ser de esta versión.
  Detalles en `docs/archive/PLAN_REDESIGN.md`.
- **Post-alpha track 2 — detekt pipeline** (cerrado): detekt
  2.0.0-alpha.3 + ktlint-wrapper + mrmans0n/compose-rules instalados.
  138 findings → 13 baselined (-91%) en 7 pasos. CI gate en PRs +
  pre-push hook local. Detalles abajo.
- **Próximo paso concreto**: Google Play Data Safety form + crear proyecto
  Supabase cloud prod (tareas humanas — release gate). En paralelo:
  instrumented E2E deletes + QA checklist multi-device. Después: tag +
  AAB. Listing en `docs/PLAY_STORE_LISTING.md`; privacidad reescrita en
  `docs/PRIVACY_POLICY.md` (pendiente hostear como Gist para URL de
  eliminación en Play).

---

## Cómo retomar después de un clear context

1. Leer este doc primero.
2. Leer `docs/ROADMAP_V1.md` (Fase 4) para el plan macro.
3. Leer `docs/POST_V1_PLAN.md` (Fase 5) para visión post-v1.
4. Si vas a S2 o más allá, leer `docs/PRODUCT_REQUIREMENTS.md` para
   los criterios de aceptación de la story que toca.
5. El design system está en `docs/DESIGN_SYSTEM.md` (incluye specs de
   componentes nuevos en §7.9-§7.11).

---

## Estado del proceso de definición (Fases 1-5)

| Fase | Doc | Estado |
|---|---|---|
| 1. Discovery + Positioning | `PRODUCT_DISCOVERY.md` | ✅ Firmada |
| 2. Requirements | `PRODUCT_REQUIREMENTS.md` | ✅ Firmada |
| 3. Architecture Review | `archive/ARCHITECTURE_REVIEW.md` | ✅ Firmada (archivada — gap analysis pre-S0) |
| 4. Roadmap v1 | `ROADMAP_V1.md` | ✅ Firmada |
| 5. Post-v1 plan | `POST_V1_PLAN.md` | ✅ Firmada |

Decisiones bloqueadas (no se renegocian sin volver a Fase 1):
- Target: peruano 25-35 con sueldo + ingresos extras.
- ~~100% local, sin login, sin cloud backend~~ — **reversada 2026-06-04
  vía ADR 001**: local-first con sync opcional (anónimo-local sigue
  siendo el default; sign-in opt-in).
- Gratis sin paywall en v1.
- Pilar diferencial: claridad de ingresos múltiples + simplicidad.
- Manifesto provocador-minimalista, enemigo: la complejidad innecesaria.

---

## Roadmap de sprints (8 totales, ~10 semanas calendario)

| Sprint | Foco | Estado | Tag |
|---|---|---|---|
| **S0** | 9 quick wins (cleanup) | ✅ Completo | `post-s0` |
| **S1** | Reporte ingresos + nav meses (US-11, US-09) | ✅ Completo | `post-s1` |
| **S2** | Onboarding + manifiesto (US-01, US-02) | ✅ Completo | `post-s2` |
| **S3** | Export/Import JSON (US-18, US-19) | ✅ Completo | `post-s3` |
| **S4** | ProfileScreen completo (US-21) | ✅ Completo | `post-s4` |
| **S5** | Polish + accesibilidad + screenshots | ✅ Completo | `post-s5` |
| **S6** | Dogfooding propio intensivo | ⚠️ Fast-tracked + mini-S6.5 cierra gate | — |
| **S7** | Testers externos + publicación alpha | 🟢 AAB listo, esperando upload Console | `2.0.0-alpha` |

---

## Sprint 0 — completado

9 commits granulares (`pre-s0..post-s0`):
- Quitado INTERNET permission + cleartextTraffic.
- Drop deps: constraintlayout, navigation legacy (fragment.ktx, ui.ktx,
  livedata.ktx).
- Removido "Cerrar sesión" del ProfileScreen.
- CLAUDE.md actualizado (legacy tables que ya no existen + referencia
  a Fases 1-5).
- Currency default `ARS → PEN` en accounts.sq.
- 3 categorías ingreso nuevas en seed: Ventas, Propinas, Otros.
- Wiring de Categorías + Cuentas en ProfileScreen.
- Versión de la app desde BuildConfig en ProfileScreen.

---

## Sprint 1 — en curso (código listo)

7 commits desde `post-s0`:
- `084aff7` feat(report): skeleton screen + Home navigation entry point
- `db1411e` feat(report): visual components — MonthSelector, Toggle, Bars
- `0d96062` feat(report): assemble screen — VM + mock data + full layout
- `05d4971` feat(report): SQL + domain models + use cases (backend real)
- `ffc6fa3` test(report): domain tests for 2 new use cases (delegado a Sonnet)
- `76bca62` feat(report): replace mock with real use cases (delegado a Sonnet)
- `5bcebac` feat(home): US-09 month navigation in HomeScreen (delegado a Sonnet)

### Lo que ya funciona (build + tests verdes)
- Pantalla Reporte E2E con data real desde SQLDelight.
- Toggle Ingresos / Gastos.
- Selector de mes (chevrons ◂ ▸) compartido entre Home y Report.
- Comparativa "+X% vs [mes]" en success/danger según signo.
- Barras horizontales con stagger 50ms al primer render.
- Empty state con CTA "Anotar ingreso/gasto".
- HomeScreen muestra balance/ingresos/gastos del mes seleccionado.

### Bug fix lateral durante S1
Sonnet detectó que `GetHomeDataUseCase` calculaba income/spend solo
sobre las últimas 7 transacciones (no sobre todo el mes). Corregido y
con test actualizado. Verificar al hacer dogfooding.

### Verificación manual S1 — pendiente (antes de tag `post-s1`)

Instalar con `./gradlew installDevDebug` y validar:

1. **HomeScreen**: chevrons cambian mes; balance, ingresos y gastos
   se recalculan. Probar wrap **enero → diciembre del año anterior**.
2. **ReportScreen con data real**: registrar 2-3 ingresos hoy, abrir
   reporte → barras con categorías reales.
3. **Toggle Ingresos ↔ Gastos**: barras y total cambian.
4. **Empty state**: navegar a un mes futuro vacío → empty state + CTA.
5. **Comparativa**: con data en 2 meses consecutivos, aparece "+X%" o
   "−X%" en verde/rojo.

### Pendiente NO bloqueante para S1
- Performance check con 1000+ transacciones sintéticas. La query usa
  índices existentes así que debería ser <100ms — si dogfooding muestra
  lag, investigar.
- Screenshots del reporte para futuro listado Play Store (cae en S5).

---

## Sprint 2 — en curso (código listo)

Commits desde `5bcebac` (US-09 nav):
- Onboarding feature completo: `AppPreferences` sobre el
  `SharedPreferences` ya existente (no se sumó DataStore — sync read
  evita el flicker que el ROADMAP marcaba como riesgo), `ManifestoScreen`
  composable stateless (sin VM porque es UI pura: 4 estrofas + 1
  botón), `ManifestoRoute(isRevisit)` en `HhRoutes`, gate de start
  destination en `Hh.kt`, "Acerca de" en `ProfileScreen`.

### Lo que ya funciona (build + tests verdes)
- Al primer launch (flag `first_launch_seen = false`), `Hh` arranca
  con `ManifestoRoute()` en vez de `START_TAB`. Sin bottom bar
  (la ruta no es `BottomBarRoute`).
- Botón "Empezar" → flag a `true` + `replaceAll(START_TAB)`.
- Desde `ProfileScreen → Acerca de` se reabre con `isRevisit=true`,
  botón cambia a "Volver", NO se reescribe el flag.

### Caveat resuelto durante S3
- `ProfileRoute` se agregó como tab del bottom bar (`HhBottomBar`) con icono de perfil
  para wirear Export/Import. El "Acerca de" pasó a ser navegable automáticamente.
  Sprint 4 (US-21 ProfileScreen completo) solo va a sumar contenido/polish
  encima de la base ya enganchada.

### Verificación manual S2 — pendiente (antes de tag `post-s2`)

1. **Primer launch**: instalar build limpio (`adb uninstall com.emm.justchill.dev`
   antes) → abrir → se ve manifesto, no bottom bar, botón "Empezar".
2. **Persistencia**: tap "Empezar" → va a Home. Cerrar app, volver a
   abrir → va directo a Home, NO se ve manifesto.
3. **Cold start**: `adb shell am start -W com.emm.justchill.dev/.MainActivity`
   → total time < 3s.
4. **Re-visit desde Profile** (solo cuando Profile esté navegable en
   S4): "Acerca de" abre manifesto con botón "Volver", al tap vuelve a
   Profile sin tocar el flag.

### Pendiente NO bloqueante para S2
- Compose UI test del `ManifestoScreen` (renderiza textos + botón).
  Cae bien en S5 polish junto con otros tests de UI.

---

## Sprint 3 — en curso (código listo)

Entregado en 3 chunks delegados a Sonnet (uno por sesión):

- **Chunk 1 — Export domain**: `BackupRepository` interface en
  `:domain/shared/backup/`, `ExportDataUseCase` (1-line delegate). DTOs
  + `kotlinx-serialization` viven en `:data/backup/` (NO en `:domain`,
  porque la regla de Clean dice que el dominio se mantiene puro JVM —
  ver `domain/CLAUDE.md`).
- **Chunk 2 — Export Android wiring**: `ProfileViewModel.exportToStream`,
  launcher `ActivityResultContracts.CreateDocument("application/json")`,
  helper `suggestedExportFilename()` con fecha. `BufferedWriter` se
  cierra **antes** que el `OutputStream` para flushear el buffer; cerrar
  el stream primero corrompe el archivo silenciosamente.
- **Chunk 3 — Import full flow**: `BackupRepository.importFromJson`,
  `ImportDataUseCase`, `ImportStats(accounts, categories, transactions)`,
  queries `deleteAll:` en las tres `.sq`. Transacción atómica vía
  `EmmDatabaseData.transaction { }`. Orden de delete por FK constraints
  (`ON DELETE RESTRICT`): transactions → categories → accounts; insert
  al revés. Validación `schemaVersion == 1` → `ValidationError` si no.
  `AlertDialog` de confirmación con copy *"¿Reemplazar tu data?"* /
  *"Reemplazar todo"* tinted en `colors.danger`. `pendingImportJson`
  vive en Compose (no en VM).

### Lo que ya funciona (build + tests verdes tras limpiar cache corrupto)
- `Perfil → Exportar` → file picker → escribe JSON con accounts +
  categories + transactions + metadata (`schemaVersion=1`, `exportedAt`,
  `appVersion`). Snackbar: *"Listo, tu data está guardada."*
- `Perfil → Importar` → file picker → AlertDialog confirmación →
  *"Reemplazar todo"* → wipe + insert atómico. Snackbar con count de
  movimientos importados. Errores: archivo corrupto / versión incorrecta
  / IO → mensajes en castellano peruano sin "Por favor".

### Decisiones de diseño que vale recordar
- **`schemaVersion=1` sin migración**: cualquier otro valor lanza
  `ValidationError`. Cuando aparezca `v2`, agregar un branch acá, no
  un sistema de migración upfront.
- **Empty payload válido = wipe vía import**: importar un JSON con
  arrays vacíos es legal y limpia toda la data (caso cubierto en tests).
- **`OutputStream`/`String` en la VM, no `Uri`/`Context`**: mantiene la
  VM Android-free y unit-testable. La conversión Uri↔Stream vive en
  `Hh.kt` con los launchers.

### Verificación manual S3 — pendiente (antes de tag `post-s3`)

1. **Export**: registrar 1-2 movimientos → `Perfil → Exportar` →
   guardar JSON en Files → abrir el JSON desde Files y verificar que
   tiene los movimientos + `schemaVersion: 1`.
2. **Import happy path**: borrar app o agregar más data → `Perfil →
   Importar` → seleccionar el JSON anterior → AlertDialog → "Reemplazar
   todo" → snackbar con count → verificar en Home que la data quedó
   reemplazada (no sumada).
3. **Import archivo inválido**: importar un `.json` cualquiera (ej. un
   `package.json`) → snackbar *"No pude importar el archivo — capaz
   está dañado."*, data NO se toca.
4. **Cancel del AlertDialog**: importar JSON válido → AlertDialog →
   "Cancelar" → no pasa nada, data intacta.

### Pendiente NO bloqueante para S3
- Compose UI test de `AlertDialog` (cancel vs confirm).
- E2E test de export → import roundtrip en `:data/src/androidTest/`.

---

## Rollback points (tags git)

| Tag | Cuándo | Comando para volver |
|---|---|---|
| `pre-s0` | Antes de ejecutar — Fases 1-5 firmadas + DESIGN_SYSTEM alineado | `git reset --hard pre-s0` |
| `post-s0` | 9 quick wins completados, repo limpio | `git reset --hard post-s0` |
| `post-s1` | Reporte + nav meses (US-11, US-09) verificado en device | `git reset --hard post-s1` |
| `post-s2` | Onboarding + manifesto (US-01, US-02) | `git reset --hard post-s2` |
| `post-s3` | Export/Import JSON (US-18, US-19) | `git reset --hard post-s3` |
| `post-s4` | ProfileScreen completo + Privacidad (US-21) | `git reset --hard post-s4` |
| `post-s5` | Polish: a11y + copy peruano + US-17 íconos por tipo | `git reset --hard post-s5` |
| `2.0.0-alpha` | AAB release-firmable. **Pre-redesign** — UI vieja. | `git reset --hard 2.0.0-alpha` |

**Próximo tag esperado**: `2.0.0-alpha.2` (o similar) cuando se
re-buildee el AAB con la UI Notion-Dark y se suba a Play Console.
Después `post-s7` cuando el alpha esté en manos de testers + primeras
24h sin crashes. Hot-fix patches irían como `2.0.x-alpha`.

### Mini-S6.5 — cierre del pre-alpha gate

Sonnet corrió un audit formal de las 15 Must vs código (commit
ae1a204 + secuencia). Encontró 3 gaps reales más 1 PARTIAL:

- **US-14** (categorías) — no había pantalla de gestión; "Categorías"
  desde Profile iba directo a AddCategoryScreen. Fix `82b57dd`:
  nueva `CategoriesScreen` con secciones INGRESOS/GASTOS, per-row
  MoreVert → Editar/Borrar, AlertDialog advertencia ("los
  movimientos pasan a Sin categoría" — coherente con FK SET NULL).
- **US-15** (cuentas) — pantalla mostraba lista pero sin edit/delete.
  Fix `6625d9f`: same patrón que B, refactor a MVI standard.
  `DeleteAccountUseCase` ya validaba "no puedes borrar con
  transacciones" — el VM surfacea el ValidationError vía snackbar.
- **US-09** (mes nav) — botón "Volver a hoy" faltaba. Fix `c0d920d`:
  TextButton condicional bajo MonthSelector, visible solo si
  `state.month != YearMonth.current()`. Wired en Home y Report.
- **US-17** (cuentas preset peruanas) — Sonnet flagueó como Must
  blocker pero PRD §10 lo lista como Should ("si llega"). NO se
  arregla en este mini-sprint — diferido a v2 o nice-to-have.

Cleanups menores anotados (no blocker):
- `libs.versions.toml` aún declara Ktor/Retrofit/Supabase/WorkManager
  sin uso real — orphans de la migración local-only.

> Items previos sobre co-localizar `CategoriesListRoute`/`CategoryRoute`
> y consolidar `CategoriesViewModel` en `HhModule` ya están resueltos
> (verificado 2026-05-21: ambas rutas viven en `ObjectsRoutes.kt`,
> todos los VMs de category están en `HhModule.kt`).

### S5 — notas

3 commits code-only delegados a Sonnet, todos build-green:
- `5dd6f17` a11y: audit per-callsite de `contentDescription`. Resultado
  sorpresa: la app ya tenía buen a11y. Solo 2 fixes ("Atrás" → "Volver"
  para normalizar). Los 24 callsites `null` restantes son genuinamente
  decorativos (icono pareado con Text — agregar description ahí solo
  ensucia TalkBack).
- `a4f5898` copy: 6 mensajes de `DomainExceptionExt` rewriteados a
  peruano coloquial, manifesto "vos" → "tú", ReportScreen
  "Anotá/volvé" → "Anota/vuelve". `DomainException.Unauthorized` no
  dispara hoy (app local-only sin auth) — el tipo queda en la sealed
  hierarchy con copy neutral, TODO en S6 evaluar si se puede borrar.
- `a0b50e9` US-17: ícono por `AccountType` en AccountsScreen
  (Bank → AccountBalance, Cash → Payments, CreditCard → CreditCard,
  Investment → TrendingUp). Extension privada en el screen — sin
  leak a `:domain` (que sigue puro JVM, sin íconos de Compose).

Device verification (user-confirmed): a11y manual OK, cold start <3s,
frame stats sin spikes evidentes, TalkBack usable, Layout Inspector
sin overdraw rojo. Screenshots de Play Store quedan para cuando se
publique en S7 (no son blocker hasta entonces).

### S4 — notas (mini scope)
- 6 de 7 ítems de US-21 ya estaban hechos en S0/S2/S3 — solo faltó
  Política de privacidad.
- `PrivacyPolicyScreen.kt` stateless (sin VM), copy de 71 palabras
  drafteada por Opus, aprobada por user. Voz peruana: "tú", no "vos".
- Pendiente para S5: el manifesto sigue usando "vos" (rioplatense).
  Bug de voz que pillamos durante S4 — corregir cuando se haga el
  pase visual + copy de S5.
- US-12 (gastos por categoría en Report) ya funciona end-to-end:
  `ToggleIncomeExpense` + `ReportViewModel.loadReport(selectedType)`
  recompone con data real al tap del toggle.
- US-17 (íconos genéricos por banco en AccountsScreen) diferido a S5.

---

## Post-alpha track 1 — Notion-Dark visual refactor (2026-05-19 → 2026-05-20)

Refactor visual completo basado en el handoff de Claude Design ("JustChill — Notion Dark", 19 pantallas / 8 componentes base / 3 bottom sheets / 3 empty states). Mantiene 100% de la arquitectura (Clean + MVI + Koin + SQLDelight) — solo capa Compose.

Plan completo y decisiones en `docs/archive/PLAN_REDESIGN.md`. Stack mapping diseño → Compose ahí.

### Lo que entró

10 SRs commiteados en `trunk` (uno por feature, sin merge — cada commit deja un dev APK iterable):

| ID | Scope | Commit |
|---|---|---|
| **SR-1** | Foundations: colors + Inter/IBM Plex Mono + radii + 10 atoms (Eyebrow, Pill, IconTile, MetaRow, JcTopBar, IconBtn, StickyCTA, AmountHero, MoneyInline, Hairline) | (foundation, no visible) |
| **SR-2** | Home + BottomBar | (varios) |
| **SR-3** | Add transaction · numpad 3×4 custom · 3 bottom sheets (account/date/category) · NoteSheet | `6278c8c`, `9254fba`, `ea20cb6`, `4454a75`, `281a8ea`, `d6c2b1a` |
| **SR-4** | Edit transaction · espejo de Add · custom delete dialog | `0b2085b`, `7fcf3e3` |
| **SR-5** | Reporte (ya estaba decente, ajustes menores) | — |
| **SR-6** | Ver: search rounded + chips hairline + grouped-by-day | `19fa27e`, `e5cd536` |
| **SR-7** | Cuentas + Nueva cuenta · atajos peruanos · Wallet type · reactive movement counts | `d041243` |
| **SR-8** | Categorías + Nueva categoría · sheet de seleccionar (deprecó `SelectCategoryScreen`) | `8f12271`, `ceec3a0` |
| **SR-9** | Perfil + Privacidad + Manifesto typography | `baba975`, `28ccc90` |
| **SR-10** | Calendar — header + month nav + 7-col grid custom (custom, no Material) | `1f32330` |

Polish posterior: `EmmSnackbarHost` full-width pill (`59a5775`), home empty states diferenciados (`64a0810`), home simplification (`a14887a`), account-picker "+ Nueva cuenta" wire (`3332a2d`).

### Implicancias para Play Store
- Cualquier screenshot ya redactado en `docs/PLAY_STORE_LISTING.md` queda **stale**.
- El AAB `2.0.0-alpha` que está tagueado es **pre-redesign**. Si querés subir alpha con la UI nueva, hay que re-buildear (`./gradlew bundleProdRelease`) y reetiquetar.

---

## Post-alpha track 2 — Tooling: detekt pipeline + CI gate (2026-05-20)

13-commit run instalando y limpiando detekt 2.0.0-alpha.3 + ktlint-wrapper + mrmans0n/compose-rules. Final: **138 findings → 13 baselined (-91%)**, build verde, CI gate en PRs, pre-push hook local.

### Setup (`bf6810e`, `a064103`, `9d1f66f`, `dc03677`, `a3263dc`)

- Plugin `dev.detekt` aplicado en root + `subprojects { }`. Versión `2.0.0-alpha.3` porque es la única que targetea Kotlin 2.3.21 (stable 1.23.8 viene con Kotlin 2.0.21 embebido).
- `detekt-rules-ktlint-wrapper` (lo que era `detekt-formatting` antes de 2.0) + `io.nlopez.compose.rules:detekt:0.5.9` como `detektPlugins`.
- `config/detekt/detekt.yml` con tweaks Compose-aware: `ignoreAnnotated: ['Composable', 'Preview']` en `FunctionNaming`, `LongMethod`, `MagicNumber`. `CompositionLocalAllowlist` configurado con los 4 locals propios (`LocalEmmColors`, `LocalEmmRadii`, `LocalEmmSpacing`, `LocalEmmType`).
- `autoCorrect = false` por default — opt-in con `./gradlew detekt --auto-correct`.
- Baselines per-módulo (`config/detekt/baseline-{app,data,domain}.xml`). 13 findings congelados, mayormente complejidad estructural (`CyclomaticComplexMethod` en `EmmButton`/`EmmTextInput`, `TooManyFunctions` en pantallas grandes).
- CI: job `detekt` paralelo a `lint`/`unit-test` en `.github/workflows/buildDev.yml`, bloquea `build`. Reportes HTML como artifact (7 días retención).
- Pre-push hook tracked en `scripts/git-hooks/pre-push` — opt-in con `git config --local core.hooksPath scripts/git-hooks` (ya activo en este local). Skipeable con `git push --no-verify`.

### Cleanup (7 pasos, commits `5a9bdaf` a `26d727e`)

| Paso | Foco | Δ findings | Commit |
|---|---|---|---|
| 1 | Renames camelCase→PascalCase + extract single-decl files + private previews | 138 → 90 | `5a9bdaf` |
| 2 | Compose API hygiene: `modifier: Modifier = Modifier` slot, param order, present-tense lambda names (`onClicked`→`onClick`) | 90 → 81 | `2ac7def` |
| 2.5 | `./gradlew detekt --auto-correct` — ktlint wrap en signatures multi-línea | 81 → 58 | `ae26b8b` |
| 3 | `rememberUpdatedState` para lambdas en `LaunchedEffect` (5 screens) | 58 → 53 | `6d9977b` |
| 4 | Exception hygiene: `@Suppress` en adapters (SafeCall, launchSafe), `cause = e` en ValidationError, `error("msg")` en lugar de `IllegalStateException()` | 53 → 43 | `93c961f` |
| 5 | Magic numbers: `CENTS_PER_UNIT`, `MAX_VISIBLE_CATEGORIES`, `SEARCH_DEBOUNCE_MS`, `CALENDAR_GRID_CELLS` | 43 → 32 | `1ecd17a` |
| 6 | Wrap líneas >120 chars (privacy policy, JSON test fixtures) | 32 → 21 | `f748556` |
| 7 | Cola trivial (`type_`→`typography`, `StickyCTA.onClick` orden, `EmmAmountChill.modifier` default) + `Compose.CompositionLocalAllowlist` config | 21 → 13 | `26d727e` |

### Caveats / decisiones a recordar

- **`ProfileViewModel` cambio de behavior** (paso 4): los `catch (Exception)` internos de `exportToStream`/`importFromJson` fueron eliminados (redundantes con `launchSafe`'s outer catch). El `onError` ahora discrimina con `when`: para export `Unknown → "No pude exportar... espacio"`, resto `→ toUserMessage()`; para import `ValidationError → toUserMessage()`, resto `→ "...archivo dañado"`. Test de `DatabaseError` en export sigue verde.
- **`ValidationError` ahora acepta `cause: Throwable? = null`** (paso 4) — cambio aditivo en `:domain/shared/error/DomainException.kt`. Permite preservar la causa cuando se traduce una excepción específica.
- **Constantes compartidas entre VMs**: `MAX_VISIBLE_CATEGORIES = 7` vive como `internal const val` en `AddTransactionViewModel.kt`, reusado desde `EditTransactionViewModel.kt` (mismo package, sin import).
- **El CI gate solo se dispara en PRs**: el workflow está configurado `on: pull_request`. Direct push a trunk (que bypasea branch protection) **no** corre CI — pero el pre-push hook sí valida localmente.

### Cómo correr / fixear

```bash
./gradlew detekt                    # corre los 3 módulos, verde con baseline
./gradlew detekt --auto-correct     # arregla lo que ktlint sepa fixear
./gradlew detektBaseline            # regenera baselines (cuando bajes findings)
./gradlew detektGenerateConfig      # regenera config default (no pisar el actual)
```

Reportes HTML en `<módulo>/build/reports/detekt/detekt.html`.

---

## Cosas que vale la pena recordar

- **Sub-agents Sonnet**: el patrón que funcionó fue delegarle trabajo
  grande con prompt detallado (referencias a archivos, criterios de
  aceptación, guardrails). Ver el prompt usado para S1 Sem 2-3 si
  necesitás repetir el patrón.
- **Sin Figma**: tokens-as-code (Kotlin `EmmColors`/`EmmType`/etc. =
  source of truth). Para pantallas nuevas, bocetos ASCII en
  `docs/PLAN_SX_*.md` (gitignored, efímeros) antes de codear.
- **Capacidad asumida**: 5-6h/sem productivas. Roadmap calibrado en
  ~10 semanas calendario. Plan B documentado en `ROADMAP_V1.md §13`
  por si hay que cortar features.
- **Política de feature requests post-v1**: `POST_V1_PLAN.md §7` —
  checklist brutal antes de aceptar cualquier feature nueva.

---

## Recordatorios y notas para el futuro Claude

- **Nunca agregar Co-Authored-By a commits** (memoria persistente).
- **Sub-agents son colaboradores** (no llamarlos "amigo del user").
- **Sprint plans** (`PLAN_S*_*.md`) son efímeros y gitignored.
  Los docs del proceso de definición sí están versionados.
  (Excepción: `PLAN_REDESIGN.md` sí está versionado — fue track largo
  y conviene tener registro. Hoy vive en `docs/archive/` junto con
  `DESIGN_BRIEF.md` y `ARCHITECTURE_REVIEW.md`.)
- Si el user pide arrancar un sprint, **leer el plan correspondiente
  en `ROADMAP_V1.md` primero** para no inventar nada.
- **detekt instalado**: si rompiste un test/build, revisar
  `<módulo>/build/reports/detekt/detekt.html` antes de pelearte con
  Gradle. El pre-push hook corre detekt — `git push --no-verify` lo
  saltea si necesitás un push de emergencia.
