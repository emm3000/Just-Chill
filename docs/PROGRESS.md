# JustChill — Progreso

> Punto de re-entrada canónico. Si retomás el proyecto después de un context
> reset, leé esto primero y después el `CLAUDE.md` del módulo que vayas a tocar.
> Si lo único que buscás es qué falta, andá directo al
> [checklist de trabajo abierto](#checklist-de-trabajo-abierto) — es la lista única, y no hay
> ningún `OPEN_WORK.md` compitiendo con ella a propósito.
>
> **Última actualización**: 2026-08-14. No se anota el hash de trunk acá: el commit que lo
> escribe ya lo deja viejo, igual que pasó con el conteo de commits.
>
> **El sync está APAGADO en producción desde el 2026-08-12.** Kill switch
> `SYNC_TEMPORARILY_DISABLED` en `presentation/.../core/sync/SyncKillSwitch.kt`. El único doc vivo
> de sync es [`docs/work/epics/E01-snapshot-backup.md`](work/epics/E01-snapshot-backup.md) — leelo
> antes de tocar cualquier cosa de sync. El forense completo —causa raíz del loop, el borrado de
> cuenta que nunca salió del teléfono— es historia y vive en
> [`docs/archive/sync/AUDIT.md`](archive/sync/AUDIT.md).
>
> Este doc se reescribió el 2026-08-08 porque quedó dos meses desactualizado y
> se perdió toda la migración KMP. El detalle histórico previo (sprints S0-S5,
> track Notion-Dark, setup de detekt, verificaciones E2E slice por slice) vive
> en el historial de git (`git log --follow docs/PROGRESS.md`) y en
> `docs/archive/`. Acá solo va lo que sigue siendo verdad.

---

## Dónde estamos ahora

App Android de finanzas personales, **local-first**, en Play Store alpha cerrada.

**No hay usuarios terceros** — ni en Android ni en iOS. Pero **el autor sí usa la app
a diario**, sobre la release que se distribuye por Firebase App Distribution, y esa
instalación tiene data real y acumulada.

Esa distinción es la que importa y este doc la perdió dos veces. Primero afirmó
"hay usuarios reales con data en el device desde `4e6de6c`", que era falso y se
corrigió el 2026-08-08. La corrección se pasó de largo: quedó como "no hay
usuarios", y de ahí se leyó una licencia para **romper data local sin costo para
nadie**. No es cierto — hay un device con data que a nadie le gustaría perder.

Lo que sí sigue siendo barato, porque no hay terceros: rehacer navegación, cambiar
UI y postergar compliance. Lo que NO es barato es una migración destructiva. Toda
migración de schema tiene que preservar, y se prueba contra
`:data:connectedAndroidDeviceTest` antes de tocar trunk — recordar que un push a
trunk distribuye por App Distribution al device del autor.

Tres tracks grandes cerrados o casi:

| Track | Estado |
|---|---|
| Producto (Fases 1-5: discovery → post-v1) | ✅ cerrado, docs en `docs/` |
| Local-first sync (slices 1-5) | ⏸ apagado desde el 2026-08-12 y **en eliminación**, no en reparación — ADR 009 lo reemplaza por respaldo snapshot; plan en `docs/work/epics/E01-snapshot-backup.md` |
| Migración KMP / Compose Multiplatform | ✅ completa y mergeada a trunk |
| Auditoría de funcionalidades | ✅ cerrada — 4 CRÍTICOS, 4 ALTOS, 3 MEDIOS |
| iOS nativo SwiftUI sobre el core KMP | ⏳ S1-S2 de 11 ✅ — plan en `docs/swiftui/PLAN.md`, ADR 005 |

**Track iOS SwiftUI** (abierto 2026-08-10, motivación: aprendizaje — ADR 005): `:presentation`
extraído (MVI + ViewModels + DI, compose-free), framework `JustChillKit` + SKIE, bootstrap SwiftUI
compilando contra el core real. CMP-iOS retirado; Android sigue en Compose sin cambios. El gate de
compile iOS vive ahora en `:domain`/`:data`/`:presentation`.

La pestaña de transacciones ahora es mensual: lista acotada al mes elegido con resumen
(ingresos/gastos/balance) y búsqueda global con tope en SQL, en vez de streamear la tabla completa.
La búsqueda y el filtro de categoría viven detrás de dos íconos en la cabecera —el sheet de
categorías se ordena por uso—, así que la pantalla arranca la lista mucho más arriba. Cuál de los
estados vacíos gana lo decide `:presentation` (`ListDisplayState`), no el `when` de Compose: es lo
que hace que la pantalla SwiftUI de S3 herede el mismo comportamiento en vez de redescubrirlo.

**Git**: la historia es lineal (0 merge commits). Nunca mergear sin `--ff-only`. No pongas acá
cuántos commits faltan pushear ni desde qué hash: el propio commit que lo escribe lo deja viejo, y
ya pasó dos veces. Sacalo del repo cuando lo necesites:

```bash
git rev-list --count origin/trunk..trunk   # cuántos faltan pushear
git rev-list --count --merges origin/trunk..trunk   # debe dar 0
```

`trunk` tiene reglas de protección (PR obligatorio + status checks). Se pueden bypassear con
permisos de admin y el push directo lo hace, pero entonces esos checks **no corrieron**: la única
verificación de ese push es la que corriste local.

Los checks requeridos estuvieron rotos y se arreglaron el 2026-08-09. Exigían `build`, `lint` y
`unit-test` — los tres jobs que la unificación del gate había borrado — así que ningún PR podía
mergear y por eso todo iba por push directo con bypass de admin. Ahora exigen **`quality-gate`** e
**`ios-compile`**, atados al app de GitHub Actions (`app_id 15368`). Si volvés a renombrar un job
de `buildDev.yml`, esto se rompe igual y en silencio: el PR queda esperando un contexto que nadie
reporta. Comprobalo con

```bash
gh api repos/emm3000/Just-Chill/branches/trunk/protection --jq '.required_status_checks.contexts'
```

Quedan dos flojeras del lado del servidor, decididas a conciencia y todavía sin tocar:
`required_linear_history: false` (nada impide un merge commit salvo tu disciplina) y
`allow_force_pushes: true` (se puede volar historia de trunk). Cambiarlas exige un `PUT` del objeto
de protección completo, no un PATCH parcial.

---

## Checklist de trabajo abierto

Todo lo que sigue abierto, en un solo lugar. Los tracks de abajo explican el **por qué** de cada
cosa; acá está el **qué falta**. Cuando cierres algo, marcalo acá y sacalo del track — dos listas
diciendo lo mismo se desincronizan, que es exactamente cómo este doc se rompió antes.

Medido el 2026-08-11 contra el código, no copiado de la versión anterior de este doc. La sección de
sync y las dos correcciones marcadas se agregaron el 2026-08-12.

### Sync — apagado en producción, y en eliminación

Apagado el 2026-08-12 por un loop de sync en producción. Desde el 2026-08-13,
[ADR 009](adr/009-backup-is-a-snapshot-not-row-replication.md) decide que **el respaldo es un
snapshot, no replicación de filas**: se sube el export JSON completo y versionado, y el motor de
replicación se **borra** — el schema de sync se queda. ADR 006 había declarado "backup, no
replicación" y dejado el motor en su lugar; de esa costura salió todo lo posterior.

El orden de construcción por fases, las trampas verificadas, las compuertas de cada fase, el forense
de los dos tenants y los hallazgos que sobreviven al motor están en
[`docs/work/epics/E01-snapshot-backup.md`](work/epics/E01-snapshot-backup.md), el único doc vivo de sync. La causa raíz del loop
y el resto de la maquinaria retirada quedan en
[`docs/archive/sync/AUDIT.md`](archive/sync/AUDIT.md): donde el archivo y ADR 009 se contradigan,
gana el ADR.

- [x] Fase 0: qué apareció en pantalla al presionar borrar la cuenta. Resuelto **sin identificar
  cuál de los cuatro candidatos disparó** — el autor no lo recuerda, y el binario original de "nada
  o un error" nunca pudo haberlo decidido: era demasiado grueso para partir los cuatro candidatos.
  "Un error" no separa la `Unauthorized` que lanza `DeleteUserAccountUseCase` (fila 2, sesión
  vencida) de la excepción client-side de postgrest (fila 3) — mensajes y causas distintas, ambos
  "un error" — y el binario no tenía ningún casillero para el `syncMutex` sin timeout (fila 4), que
  ni es "nada" ni es "un error" sino un estado atascado a medio camino ("Eliminando…" para siempre).
  Se cerraron los cuatro y el camino ahora es observable en vez de intentar identificar al culpable.
  AUDIT §8.
- [x] Fase 0: que el borrado reporte su falla; rama faltante de `SessionRequiredException` en
  `toAuthDomainException`. AUDIT §8.
- [ ] `DefaultAuthRepository.deleteAccount()` tiene el mismo defecto que Fase 0 corrigió en
  `signOut()`: llama `client.auth.signOut(SignOutScope.LOCAL)` sin capturar el fallo de red después
  del RPC `delete_account`, y su propio KDoc afirma que "clears the on-device session" — falso si la
  red cae en el medio. No corregido en Fase 0 a propósito; ADR 009 Fase 5 ya reescribe
  `DeleteUserAccountUseCase`, y es ahí donde entra.
- [ ] Fase 0: decidir qué se hace con los dos tenants. AUDIT §8, §10.
- [ ] **Fase 1: decidir el fork de scoping por usuario** — DB por usuario, filtro `userId` en cada
  lectura, o wipe al cambiar de cuenta. Sin decidir. AUDIT §5 (Identity).
- [x] ADR 009 Fase 1 — **en `trunk` y pusheada** el 2026-08-14. Aterrizó completo: v2 congelado con
  su fixture y su test, `recurring_movements` en el export a formato v3, y el barrido del import
  gateado por `BACKUP_RECURRING_SINCE_VERSION` — un archivo que declara 3 o más barre y restaura esa
  tabla, uno v1/v2 no la toca. El restore trae `createdAt` y `lastConfirmedPeriod` del archivo,
  `ImportStats.recurring` cuenta lo que aterrizó y el diálogo de import ya nombra la tabla que puede
  borrar. `qualityGate` verde, compile iOS incluido. Era **precondición dura**: sin esto un restore
  perdía los movimientos recurrentes. La rama `adr-009-phase-1-export-v3` quedó apuntando al mismo
  commit que `trunk`; no la leas como trabajo pendiente. Fase 2 ya está destrabada
  (`docs/work/epics/E01-snapshot-backup.md`).

  **Esta entrada estuvo mal durante un día y vale la pena saber por qué**: el commit que la escribió
  (`17fc33b2`, "record ADR 009 Phase 1 as finished on the branch, not shipped") era verdad al
  escribirse y falso una hora después, cuando la rama se integró. Un doc que afirma el estado de
  integración de una rama caduca en el merge siguiente — la misma clase de dato que la cabecera de
  este archivo ya prohíbe anotar (cuántos commits faltan pushear, desde qué hash). Sacalo del repo:
  `git branch --contains <sha> -a`.
- [ ] ADR 009 Fases 2-5: pipeline de snapshot, visibilidad, confianza en el restore, y recién ahí
  desmantelar el motor. Detalle y compuertas en `docs/work/epics/E01-snapshot-backup.md`.
  - [x] **Fase 2a — export transaccional**, en `trunk` el 2026-08-14 (`7a16ce19`, `fae2e147`,
    `7bfa16c2`), **sin pushear**. `exportToJson` hacía cuatro `Flow.first()` independientes y una
    escritura entre dos producía un archivo que describe un estado que la base nunca tuvo; ahora las
    cuatro tablas se leen con `executeAsList()` dentro de un solo `transactionWithResult`. El export
    quedó envuelto en `safeDbCall` — al leer `db` directo se perdía el `catchAsDomainException()` que
    aplicaban las interfaces de repositorio, y una excepción cruda de SQLite escapando del export no
    la agarraba ningún test — y recuperó el hop de `ioDispatcher` que vivía dentro de
    `mapToList(ioDispatcher)`. El constructor bajó a `db` + `clock`: las cuatro repos solo las usaba
    el export, y con ellas se fue `allLive()` entero (interfaz de `:domain`, impl, data source y
    fake). `DefaultBackupRepositoryTest` pasó de mockear cuatro repos a correr contra un
    `JdbcSqliteDriver` real: los 8 comportamientos que pinneaba siguen pinneados, más 3 nuevos.
    Revisado en contexto fresco con el gate corrido desde cero y **verificación por mutación** —
    sacar el `safeDbCall` y la transacción tira exactamente los dos tests nuevos.
    **Smoke test en device, 2026-08-14, `emulator-5554`**: el gate nunca ejerció esto contra SQLite de
    Android — los host tests corren sobre `JdbcSqliteDriver` (JVM) y el device usa
    `AndroidSqliteDriver`, que lleva la transacción en un `ThreadLocal`. Se exportó por UI y los cuatro
    conteos del archivo coinciden con los del SQLite del device (1 cuenta, 2 categorías, 1 movimiento,
    3 plantillas), `schemaVersion` 3, las cuatro arrays presentes, arranque sin
    `NoDefinitionFoundException` — el riesgo real del constructor que pasó de seis dependencias a dos —
    y cero excepciones en logcat. Observado, no razonado.
  - [x] Fase 2b — storage. **Partida en dos**, porque el plan la escribió como una sola unidad y
    mezcla tres cosas que fallan distinto: una dependencia, una migración de servidor y el primitivo
    de integridad. El mismo criterio con el que el plan ya parte la Fase 2 en tres series de PR.
    - [x] **2b-i — el primitivo de integridad**, en `trunk` el 2026-08-14 (`5d43cc90`, `7dc783f0`,
      `d0b9a871`, `8e343996`), **sin pushear**. Cero red, cero servidor: se landeó solo y primero, para
      que un hash que no coincide y una falla de red no se depuren juntos más adelante. okio quedó
      **declarado** en `libs.versions.toml` en vez de heredado por transitividad de `supabase-kt` — la
      integridad de los respaldos no se apoya en una dep que llega de rebote, y verificado en un
      worktree aparte el pin no mueve la resolución (Gradle ya elegía 3.17.0 por highest-wins). Con
      okio no hace falta `expect/actual`: `ByteString.sha256()` compila igual para Android e iOS desde
      commonMain. `sha256Hex` toma **bytes, no String** — el upload manda bytes y la verificación
      post-read-back tiene que hashear la secuencia idéntica. `BackupManifestDto` lleva dos versiones
      que no se pueden confundir: `manifestVersion` (suya) y `payloadSchemaVersion` (la del snapshot
      que describe). Los 18 tests viven en `commonTest`, así que también compilan para iOS.
    - [x] 2b-ii — storage. **Partida en dos también**, por donde ya corta el borde cliente/servidor:
      la infraestructura se landea y se sondea sin una sola línea de pipeline, y un bucket que
      rechaza lo que no corresponde conviene tenerlo antes de que algo escriba en él.
      - [x] **Parte A — bucket y plugin**, en `trunk` el 2026-08-14 (`18ac5838`, `30098d14`,
        `04cae52c`, `74a0bdea`), **sin pushear**. `storage-kt` al catálogo, `install(Storage)` en
        `SupabaseModule` con `requireValidSession = true`, y la migración
        `20260814200043_backup_storage_bucket.sql`: bucket `backups` privado, tope de 10 MiB, sólo
        `application/json`, y políticas select/insert/delete `to authenticated` sobre el primer
        segmento del path (sin update: cada snapshot lleva su timestamp, ninguna key se reescribe).
        El predicado se sondeó contra un stack local con dos usuarios autenticados reales y no se
        pudo cruzar. Tres trampas quedan anotadas en `docs/work/epics/E01-snapshot-backup.md` porque condicionan la
        parte B y la 2c: `storage.protect_delete()` bloquea el DELETE por SQL (la poda de la 2c va
        por la Storage API sí o sí), el mime se compara literal — `application/json; charset=utf-8`
        devuelve HTTP 415 — y el upload resumable está cerrado por RLS. El `on conflict` pasó de
        `do nothing` a `do update`: con `do nothing` la migración reportaba éxito y no corregía
        nada sobre un bucket ya existente, probado rompiendo las tres columnas a mano. Nada en CI
        protege el predicado RLS; eso queda diferido a la Fase 4.
      - [x] **Parte B — upload con read-back verificado**, en `trunk` el 2026-08-14 (`e0aef49a`,
        `192e6032`, `8335cf29`, `08267bdd`, `56be26e0`, `4b56c131`, `66f499bf`, `4359e11c`),
        **sin pushear**. `BackupUploader`
        (puerto en `:domain`), `DefaultBackupUploader` (sube → relee → verifica → manifest) y
        `BackupObjectStore` + `SupabaseBackupObjectStore` (la costura de cuatro operaciones que hace
        demostrable un mismatch en el host suite, sin red ni proyecto Supabase). El orden es el
        diseño: **que exista el manifest ES la afirmación de que el payload de al lado se verificó**,
        y eso se sostiene porque el bucket no otorga `update` y el upload va con `upsert = false`.
        La verificación son bytes crudos de los dos lados (`sha256Hex(leídos) == payloadSha256`),
        nunca los `rowCounts` — salen de los mismos bytes que el digest. **El manifest también se
        relee**, por igualdad de bytes: confiar en su 200 es justo lo que esta unidad existe para
        rechazar, y un manifest corrupto en tránsito condena un snapshot sano en la verificación
        previa a un restore. Si el manifest no coincide se borran los dos objetos. Una relectura que
        FALLA no borra nada — el transporte muerto que impidió leer impediría borrar, y la razón
        precisa se perdería. La espera de sesión quedó acotada a 10 s (el `launchOp` de la 2c la
        sostendría sin excepción ni mensaje); los timeouts que ADR 009 difiere son
        `requestTimeout` / `transferTimeout`, que no gobiernan esto. Un hash que no coincide ya no
        usa `BackupFileInvalid` — ese texto es para quien eligió un archivo malo al *restaurar* —
        sino `BackupUploadUnverified`. **Verificación por mutación** de las diez conductas nuevas,
        incluidas las tres del contrato con el servidor (id del bucket, `upsert = false`,
        `application/json` pelado) que hasta ahora sólo vivían en prosa: escribir `"backup"` en
        `BACKUP_BUCKET_ID` compilaba y pasaba todos los tests del repo. Cerró además un **flake en el
        gate** que sólo aparecía con `qualityGate --rerun-tasks` en frío y en paralelo:
        `SupabaseBackupObjectStoreTest` importaba la sesión sobre un plugin de Auth que todavía no
        terminaba de inicializar, y la escritura de estado de `init` podía aterrizar última. Un flake
        en el gate es peor que un rojo — enseña a re-correr.
  - [x] Fase 2c — orquestación: dirty flag, `backgroundEvents()`, "Back up now", retención,
    flag `SNAPSHOT_BACKUP_ENABLED`. **Cerrada** — las cuatro sub-unidades abajo landearon en `trunk`,
    sin pushear. **La poda se apoya en `<name>.manifest.json`, nunca en `<name>`
    solo**: un payload sin sidecar no ocupa cupo de retención y se borra a la vista. Una poda por
    nombre le da cupo a un huérfano y desaloja un snapshot verificado — eso es pérdida de datos, y
    el razonamiento completo (incluido cuál huérfano bueno se tira a propósito) está en la fila
    Retención de `docs/work/epics/E01-snapshot-backup.md`.
    - [x] **2c-i — el watermark de cambios locales y el timestamp persistido**, en `trunk` el
      2026-08-14 (`646e2666`, `1da00cfa`), **sin pushear**. `BackupRepository.latestLocalChangeAt():
      Long?`, un solo `UNION ALL` en `backup.sq` que reduce las cuatro tablas a una lectura — no
      cuatro — por la misma razón que la Fase 2a movió el export a `transactionWithResult`. Cuenta
      las filas soft-deleted a propósito, al revés que el export: `softDelete` también pisa
      `updatedAt`, así que un borrado es un cambio que amerita respaldo. `null` (las cuatro tablas
      vacías) se distingue de `0` de forma estructural, no por convención — SQLDelight genera
      `Long?` para la columna. Del otro lado, `AppPreferences.lastSuccessfulBackupAt` por usuario,
      con el mismo sentinel `-1L` que `lastSyncedAt`. Nadie consume ninguno de los dos todavía; la
      2c-iii es la que los compara. `DefaultBackupRepository` ya estaba en el techo de
      `TooManyFunctions` de detekt (11), así que agregar el override forzó extraer el
      `usableCategoryId` privado que ya existía a una función top-level file-private, con la misma
      forma de extensión que `snapshot`. El watermark ya tiene costura propia: `BackupMetadataStore`
      en `:domain` (`lastSuccessfulBackupAt` / `setLastSuccessfulBackupAt` / `clear`), implementada
      por `DefaultBackupMetadataStore` en `:presentation` (`core/backup/`) y registrada en
      `BackupModule.kt` — landed como **2c-iii-a** (`ab7d90e7`, `ac2bed36`, `0acd47ec`, `3409560f`).
      El clear salió de `DefaultSyncCursorStore.clear` y es su propio paso en
      `DeleteUserAccountUseCase`, dentro del mismo bloque `NonCancellable`. Detalle completo en
      `docs/work/epics/E01-snapshot-backup.md`.
    - [x] **2c-ii — naming del snapshot y poda de retención**, `trunk` (`3e3c5dd9`, `b1c2e3ce`,
      `2a186ebe`, `217d4081`, `0c510b2a`, `05296659`, `95b7bc90`, `559cf2e7`, `955b8797`).
    - [x] **2c-iii-a — las costuras** (`resumeEvents`/`backgroundEvents`, `BackupMetadataStore`),
      `trunk` (`ab7d90e7`, `ac2bed36`, `0acd47ec`, `3409560f`).
    - [x] **2c-iii-b — el orquestador**, `trunk` (`e5989df4`, `8ab93972`, `d7fa1cff`, `e7fc821d`,
      `1a8df300`).
    - [x] **2c-iv — la acción manual "Respaldar ahora", Fase 2c cerrada**, `trunk` (`c8a0540f`,
      `44461895`, `e1b90622`).
- [ ] `buildBackupManifest` lanza `ValidationCode.BackupFileInvalid` — "El archivo está dañado o no es
  un respaldo de JustChill", la voz del *restore* — ante un defecto de nuestro propio export, donde
  no hay archivo que nadie eligió. El arreglo honesto es `DomainException.Unknown`, no un
  `ValidationCode` nuevo: nada de esto es input del usuario. Es una unidad aparte a propósito —
  `BackupManifestTest` pinnea esos mensajes y tocarlo desde la 2b-ii parte B ensanchaba el diff sobre
  la superficie ya shippeada y testeada de la 2b-i. Anotado acá para no redescubrirlo por tercera vez.
- [ ] **`appVersion` está hardcodeado `"1.0.0"` en iOS** (`presentation/src/iosMain/.../KoinIos.kt:49`,
  `single(named("appVersion")) { "1.0.0" }`). Preexistente — hasta ahora solo pintaba el footer de
  Perfil, donde una versión falsa es cosmética. Con la 2c-iii-b dejó de serlo: `BackupOrchestrator`
  estampa ese mismo valor en el payload del snapshot, así que **un backup escrito desde iOS miente
  sobre la versión de la app que lo produjo** — y esa versión es justo lo que se va a mirar el día
  que un restore no cuadre con el schema. **No se arregla acá a propósito**: leer la versión real es
  `NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString")`, plataforma pura,
  y es su propia unidad. Anotado con la consecuencia dicha para que no se descubra durante un
  restore.
- [x] ~~La app tiene que decir en pantalla, antes del primer upload, que sube el ledger entero del
  device~~ — aterrizó como ADR 009 unit 3c (`3ce1dfb0`, `df452737`, `8b12fc45`);
  `docs/work/epics/E01-snapshot-backup.md`, crónica en `docs/archive/sync/`.
- [x] ~~Fase 2 (upsert condicional del server)~~ — **cancelado** por ADR 009 Decision 2: era el
  arreglo de un protocolo de replicación que deja de existir.
- [x] ~~Fase 3: rediseño de bordes, colapsar los cuatro `*TableSync`, cursor a SQLDelight~~ —
  **cancelado** por ADR 009: el motor se borra en vez de rediseñarse. La restricción vinculante de
  AUDIT §9 (cero cambios de `CREATE TABLE`, a lo sumo una migración aditiva) sigue en pie y ahora
  la lleva ADR 009 Decision 9, para cuando algún día se reabra sync.
- [ ] Arreglar los hallazgos vivos que sobreviven al backup-only: cursor único para cuatro tablas,
  push sin batching (con un punto **sin verificar** sobre el timeout), livelock de
  `MAX_PULL_PAGES`, pérdida silenciosa por fecha fuera de rango, tres carreras en
  `SyncOrchestrator`, iOS sin collector de `_events`. AUDIT §5.
- [ ] Cero tests de push, tres tests tautológicos, y hacer escribible el test que lo habría
  cachado. AUDIT §5 (Tests).
- [ ] Limpieza de tenants, **no ejecutada**: limpiar los dos y volver a subir desde el device.
  Prerrequisito duro: verificar antes que el export local funciona y el SQLite del device está
  intacto. AUDIT §10.
- [ ] Detector de drift en CI con el md5 normalizado del schema del server, y mover el guard de
  `relreplident` después del `continue` de idempotencia en
  `supabase/migrations/20260812051050_composite_primary_keys.sql` (ambos de antes de esta sesión).
- [ ] `SyncMutex.withLock` no tiene timeout: un ciclo de sync trabado bloquea el borrado de cuenta
  indefinidamente (AUDIT §8, candidato 4). **Hoy es inerte** — el sync está apagado por el kill
  switch — pero hay que cerrarlo antes de reactivar el sync.
- [ ] `toSyncDomainException` (en `DefaultSyncRepository.kt`) mapea `SessionRequiredException` a
  `NetworkUnavailable`, mostrando "Sin conexión" para lo que en el auth path se trata como un
  problema de sesión. Es intencional, no un olvido — el comentario sobre esa rama explica por qué
  (transitorio, no debe cerrar la sesión) — pero sigue siendo una divergencia con
  `toAuthDomainException` (AUDIT §8). Revisar y confirmar que sigue siendo la divergencia deseada,
  no "corregirla" como si fuera un defecto.
- [ ] `toAuthDomainException` mapea `SessionRequiredException` a `Unauthorized` asumiendo que el
  delete path llama `observeSession.awaitInitialization()` como `DefaultSyncRepository`, pero no lo
  hace — la carrera de Kotlin/Native sigue abierta y se muestra como error de credenciales en vez de
  reintentable. El fix (agregar el await) es un cambio de comportamiento aparte. Razonamiento
  completo: KDoc de `toAuthDomainException` en `DefaultAuthRepository.kt`.

### Fechas — lo único abierto que toca el servidor y la data real

- [ ] **Fase dos del hallazgo #5**: migrar la columna de Supabase de `date bigint` a `text`, UTC-5
  (no `AT TIME ZONE 'UTC'`) — SQL ya en verde, 7/7 vectores, falta el secuenciamiento humano sobre
  data real. Solo borra el caller de sync en `FixedPeruOffset.kt`; el caller que lee un backup v1
  es permanente. Detalle: [`docs/archive/sync/AUDIT.md`](archive/sync/AUDIT.md) §5.

### Release y compliance — bloqueantes del alpha, solo los puede hacer un humano

- [x] **El proyecto Supabase cloud de prod ya existe** — corregido el 2026-08-12; esta línea decía
  que faltaba crearlo. Es `pievwpleqmrjwszuuivr` ("Justtt"), linkeado desde el 2026-06-10
  (`supabase/.temp/linked-project.json`), con las tres migraciones aplicadas y los `prod.*`
  poblados en `supabase.properties`. La confusión no era gratuita: ese server tiene dos tenants con
  data real y filas cruzadas. Ver [`docs/work/epics/E01-snapshot-backup.md`](work/epics/E01-snapshot-backup.md), sección
  "Production forensics".
- [ ] Hostear `docs/PRIVACY_POLICY.md` como URL pública (Play la exige para apps con eliminación
  de cuenta).
- [ ] Completar el Google Play Data Safety form.
- [ ] Checklist QA: clean install, semana offline-first, sign-in tardío, sign-out, y **upgrade real
  con APK viejo + `adb install -r`**. La parte de "dos devices" queda **sin objeto**: la decisión de
  backup-only del 2026-08-12 dice un device a la vez, así que no hay convergencia multi-device que
  probar.
- [ ] Corregir la declaración de advertising ID en Play Console: hoy dice "Yes" y es falso. Se puso
  así durante la subida de `v2.4.0` para destrabar un rechazo. `docs/PLAY_ADVERTISING_ID.md` tiene
  la evidencia y los comandos que la reproducen sobre cualquier AAB — leelo antes de flipear.
- [ ] `v2.4.0` está tagueado y construido pero **nunca llegó a la pista alpha**. Un workflow verde
  no publica nada; el borrador se publica a mano en Play Console.
- [ ] Reescribir la ficha de `docs/PLAY_STORE_LISTING.md`: la descripción larga todavía promete
  "Sin login. Sin servidor." y "No te sincroniza con la nube", y el ADR 001 lo contradice — hoy hay
  sync opcional con Supabase y cuenta. Publicar eso tal cual sería declarar algo falso en la ficha,
  justo al lado del Data Safety form.

### Detekt y gate

- [x] Los **15** errores de compilación de `:ui-android:detektMainAndroid`, cerrados el 2026-08-11
  borrando el registry de SavedState de navegación (`3105d91`, `bb9e6d5`, `57fe356`). La tarea
  ahora reporta cero.
- [ ] **15 errores de expect/actual** (`:data` 9, `:presentation` 6): detekt analiza commonMain y
  androidMain como una sola unidad, sin fragmentos HMPP, así que cuenta **3 errores por par** y
  corre **degradada** (sin type resolution) sobre esos archivos — un `detektMainAndroid` verde no
  prueba lint completo. Crece +3 por cada `expect/actual` nuevo; nada lo topa.
- [ ] **13 errores más en `:androidApp:detektDevDebug` y `detektDevRelease`**, sin diagnosticar y
  sin cambio antes y después del trabajo del 2026-08-11. Las variantes `prod*` reportan 10. Los tres
  de diferencia salen del flavor `dev` — probablemente del playground `experiences/`, que solo
  existe ahí — pero eso **no está verificado**: detekt no imprime los mensajes sin `debug = true`
  y con eso tampoco los soltó. Diagnosticar es parte de esta tarea, no un dato ya conocido.
- [ ] **El gate no falla con errores de compilación de detekt.** detekt los degrada a warning y la
  tarea termina en `BUILD SUCCESSFUL` — medido. Lo que importa no es el ruido en consola sino que
  cualquier regla que dependa de type resolution puede no dispararse, en silencio.
- [ ] El modo compiler-plugin de detekt arreglaría de raíz los expect/actual de arriba, pero no es
  viable hoy: **(1)** `dev.detekt.gradle.compiler-plugin` declara configuration-cache `UNDECLARED`,
  y choca con `org.gradle.configuration-cache=true` (`gradle.properties:24`); **(2)** en
  `2.0.0-alpha.6` el artefacto declarado 404-ea en Maven Central — el real es
  `2.4.10-2.0.0-alpha.6` — así que resolver `kotlinCompilerPluginClasspath` exige forzar la versión
  por substitución. Vía cerrada, no una regresión.
- [ ] Purgar las **47** entradas muertas de `UnusedPrivateFunction` en
  `config/detekt/baseline-ui-android-main.xml` (sobre 154 entradas en total). Desde `c94e290` la
  regla ignora los `@Preview` por anotación, así que esas entradas quedaron inertes.
- [ ] Entrada `ImportOrdering:ProfileScreen.kt` en `config/detekt/baseline-ui-android-main.xml:37`,
  probablemente muerta desde que `253e170` tocó esos imports. **No verificado**: correr la tarea y
  ver si el issue reaparece antes de borrarla.
- [ ] Sacar el `@Suppress("CyclomaticComplexMethod")` de `ui-android/.../ProfileScreen.kt:335` al
  borrar el kill switch — cubre todo `AccountSection` en vez de solo las ramas de sync. Única
  SUGGESTION del Judgment Day de `253e170`.
- [ ] **Burn-down de los 7 `TooManyFunctions` con amnistía** en
  `config/detekt/baseline-ui-android-main.xml`, contra el umbral de 8 funciones top-level no-`@Preview`
  por archivo: `SeeTransactionsScreen` (16), `HomeScreen` (16), `AddCategoryScreen` (13),
  `AccountsScreen` (11), `AddEditRecurringMovementScreen` (10), `ProfileScreen` (15),
  `RecurringMovementsScreen` (9). La entrada del baseline no lleva el conteo, así que **el gate no los
  va a volver a reportar nunca**, crezcan lo que crezcan: si no se bajan acá, no se bajan.
  Criterio y método de conteo en `docs/CODE_QUALITY.md`.
- [ ] `LongParameterList:ProfileScreen.kt:@Composable private fun ProfileRowWithTrailing` en
  `config/detekt/baseline-ui-android-main.xml:65` — 8 parámetros contra el tope de 5 para funciones
  (`config/detekt/detekt.yml`), name-keyed y sin conteo, misma amnistía permanente que el punto
  anterior. Burn-down requerido por `docs/CODE_QUALITY.md` (arbitraje: crecimiento de baseline solo
  entra junto con esta línea).
- [ ] `:ui-android:detektAndroidMainSourceSet` reporta **21** issues. Preexistente y deliberadamente
  fuera del gate: `detektMainAndroid` cubre los mismos archivos **con** type resolution, así que
  sumarlo serían más tareas y no más cobertura — el razonamiento está en `QualityGateConventionPlugin`.
- [x] `GenerateBuildInfoTask.UNKNOWN_COMMIT` es la tercera copia de la palabra `"unknown"`, y ahora
  **cada copia tiene su propio test que la deletrea**: `CommitHashUiTest` (`GENERATOR_SENTINEL`) fija
  el lado de `:ui-android`, y `GenerateBuildInfoTaskTest.the sentinel is the exact word the app side
  spells out` fija el del generador con un `assertEquals("unknown", UNKNOWN_COMMIT)`. La entrada
  anterior decía que un test de build-logic "no puede cerrar esta mitad" porque `GenerateBuildInfoTaskTest`
  **lee** la constante en vez de deletrearla — eso describía los tests que había, no un límite: un
  test puede deletrear la palabra igual que lo hace el otro módulo. Sigue sin haber nada que **linkee**
  las dos constantes (`build-logic` no está en el compile classpath de la app), así que cambiar la
  palabra de verdad son cuatro ediciones: las dos constantes y los dos tests. Verificado por
  mutación: escribir `"unknwon"` en `UNKNOWN_COMMIT` pone `:build-logic:test` en `FAILED`.
  La otra mitad **también está cerrada**: `build-logic` ya tiene source set de tests y cuelga de
  `qualityGate`. `normalizeCommitHash` tiene ocho tests —sha válido, mayúsculas, newline final,
  whitespace alrededor, comillas/backslash/`$`/newline, sufijo `-dirty`, sha corto, vacío— y el gate
  los corre como `:build-logic:test` (`build.gradle.kts` raíz aplica `justchill.quality.gate` solo
  para eso; `build-logic` es un included build y el matcheo por nombre de tarea no lo alcanza).
  Verificado por mutación: ensanchar el regex a `[0-9a-f]{40}(-dirty)?` pone `./gradlew qualityGate`
  en `BUILD FAILED` con `Execution failed for task ':build-logic:test'`.
- [ ] El **template** de `GenerateBuildInfoTask.generate()` no tiene test: `normalizeCommitHash` ya
  está cubierto, pero borrar las comillas de `"$full"` en el `trimMargin()` no pone en rojo nada
  dentro de `build-logic`. Lo agarraría `:androidApp:compileDevDebugKotlin`, que no es lo mismo que
  un test y no dice qué cambió. Cerrarlo pide invocar la tarea de verdad (`ProjectBuilder` o
  `GradleRunner`) contra un directorio temporal y leer el archivo generado — más maquinaria de la
  que correspondía meter en la limpieza que agregó el source set. Omisión elegida, no accidental.
- [ ] `build-logic` **corre en el gate pero no se lintea**: no aplica detekt (su build file aplica
  solo `kotlin-dsl`; la entrada de detekt ahí es un marker `implementation` para poder *escribir*
  `DetektConventionPlugin`). `GenerateBuildInfoTaskTest.kt` es el único archivo que el gate ejecuta
  y nunca analiza.

### Docs y comentarios que afirman cosas falsas

- [ ] `ui-android/.../hh/shared/AppNavHost.kt:51-67` — cabecera de 17 líneas que describe un host
  que ya no existe: dice CMP host único para Android+iOS, port navigation3-UI de JetBrains, y
  `PlatformHostActions + startTab` tras `expect/actual` — las tres son falsas hoy (`:ui-android` es
  Android-only, el port se retiró, `PlatformHostActions` es `interface`/`val` planos en
  `hh/shared/PlatformHostActions.kt:39`/`:168`). Sin tocar todavía; re-verificar el rango de línea
  en cada edición del archivo.
- [ ] `docs/DESIGN_SYSTEM.md` §5 documenta 5 radios con otro esquema de nombres (`radius.0`,
  `radius.s` 6dp, `radius.m`, `radius.l`, `radius.full`); `EmmRadii.kt` ships **9** (`r0`, `rXS` 8dp,
  `rS` 10dp, `rM`, `rL`, `rXL`, `rXXL`, `rLTop`, `rFull`) y `rXS` —el que usa el footer de commit— no
  tiene fila en el doc. Los nombres no coinciden en ningún caso; los dp coinciden en tres de cinco
  (`radius.0`/`r0` 0, `radius.m`/`rM` 12, `radius.full`/`rFull` 999) y divergen en dos:
  `radius.s` 6 contra `rS` 10, y `radius.l` 20 contra `rL` 14 —los 20dp del doc son los de `rXXL`.

### Bugs

- [x] **Crear una categoría desde un movimiento de Ingreso abre el formulario en Gasto.** Cerrado por
  schema, no por parche: la categoría de un movimiento ahora es **foreign key compuesta**
  `(categoryId, type) → categories(categoryId, categoryType)` en `transactions` y en
  `recurring_movements`, con la migración `4.sqm` (schema v5) y
  [ADR 008](adr/008-the-schema-owns-the-category-type-invariant.md).
  El default de `CategoryRoute.initialType` era el disparador, pero no la causa: **nueve** escritores
  podían meter el par y **tres** ni siquiera pasan por `:domain` (`DefaultBackupRepository` en import,
  `TransactionTableSync` y `RecurringMovementTableSync` en el pull), así que cualquier invariante
  puesta en `:domain` tenía techo por diseño — por eso cuatro rondas de parches en runtime no lo
  cerraron. Esos parches quedaron **descartados**, solo en el tag `patches-descartados-2026-08-12`;
  no resucitarlos.
  Lo que sí se hizo del lado UI: el push desde el formulario de transacción propaga el tipo del
  movimiento, el picker y el formulario de recurrentes solo ofrecen categorías del tipo elegido, y
  cambiar el tipo en recurrentes suelta la categoría que ya no aplica. Eso es UX — el mecanismo es
  la base.
  **Las filas corruptas SÍ se reparan**: la migración pone `categoryId = NULL` en todo movimiento
  cuya categoría sea de otro tipo o no exista, en las dos tablas. Nunca toca `type` — el `type`
  firma el `amount`, así que darlo vuelta reescribiría el balance del usuario. Y no marca las filas
  como `Pending`: con el sync apagado y en rediseño backup-only (ADR 006) no hay a dónde propagar la
  corrección, y solo inflaría el primer diff de backup.
  **Corrido en device: 34/34 verde**, el 2026-08-12 en `medium_phone` (emulator-5554, API 36).
  `MigrationV4ToV5Test` 13/13, `SyncFkExceptionTest` 3/3, cero fallas, cero errores, cero ignorados.
  Dos de los trece migran con foreign keys ON — la configuración de iOS, la única bajo la que el
  orden de `4.sqm` importa: invertirlo (reconstruir antes de reparar) hace fallar exactamente esos
  dos y deja verdes a los otros once.
  Ensayo extra sobre un archivo `.db` real (no un fixture): la base v4 del emulador, con sus 23
  categorías y sus índices, migrada con `PRAGMA foreign_keys=ON` — sin error, `foreign_key_check`
  vacío, conteos intactos, FK compuesta declarada y los 12 índices presentes.
  **EL BACKUP NO ERA RED COMPLETA PARA ESTA MIGRACIÓN.** Medido contra la 2.4.0, que escribe formato
  v2: `ExportPayloadDto` llevaba `accounts`, `categories` y `transactions`, y **no llevaba
  `recurringMovements`** — verificado exportando desde la 2.4.0 en el emulador: el JSON no tiene la
  clave. **Eso ya cambió**: el formato v3 (`BACKUP_SCHEMA_VERSION = 3`, en `trunk`
  desde el 2026-08-14) sí lleva `recurringMovements`, y `ExportPayloadDto.recurringMovements`
  lo declara. La medición de
  arriba sigue describiendo los archivos escritos por la 2.4.0, que son los que están en disco hoy.
  Pero `4.sqm:60-65` sí nullea `recurring_movements.categoryId`. O sea, cuando esta migración corrió:
  la categoría que perdiera una plantilla recurrente **no estaba en ningún backup y no se podía
  recuperar**, y hasta que alguien la re-asigne a mano cada confirmación mensual acuña un movimiento
  sin categoría. Los movimientos recurrentes con el par mismatched tampoco se podían CONTAR desde el
  backup, por lo mismo. La única medición previa posible era a ojo, en la pantalla de recurrentes,
  antes de instalar. Con v3 en adelante un backup nuevo **escribe** esas plantillas en el archivo, y
  el import de un archivo que declara 3 o más **las barre y las restaura** — con `createdAt` y
  `lastConfirmedPeriod` del archivo, no del import. Un archivo v1/v2 no toca esa tabla: no tiene
  plantillas que devolver. La mitad de UI (`ImportStats.recurring` y el diálogo) ya aterrizó; el
  detalle queda en `docs/work/epics/E01-snapshot-backup.md`.

- [ ] **Editar un movimiento de una categoría borrada lo re-archiva bajo otra, sin que nadie lo elija.**
  `EditTransactionViewModel.resolveSelection` (`:109-112`) cae en `?: list.firstOrNull()` porque
  `categories.sq:all` filtra `deletedAt IS NULL` y una categoría tombstoneada no está en la lista,
  perdiendo el vínculo que `DeleteCategoryUseCase` preserva a propósito (`RecurringMovementFkTest:119-132`).
  Preexistente, no lo introdujo el FK compuesto (ADR 008).

### Deuda técnica

- [x] ~~`SyncLogger` está nombrado para el sync path, pero ya es el canal general de diagnóstico~~ —
  **cerrado en ADR 009 2c-iii-b.** Es ahora `DiagnosticsLogger`, en `domain/.../shared/logging/`, con
  `CrashReportingDiagnosticsLogger` (Android) y `PrintlnDiagnosticsLogger` (iOS). Lo que lo forzó no
  fue el nombre desalineado sino la Fase 5: borra todo archivo, paquete y módulo con nombre de sync,
  y el `BackupOrchestrator` necesita este port para cumplir la restricción dura 4 (ningún fallo
  silencioso). Depender de un símbolo con nombre de sync desde el pipeline que sobrevive no era
  aceptable, así que el movimiento se adelantó a la fila "Keep, renamed" de la Fase 5. Tocó 21
  archivos, todos mecánicos y verificados por el compilador. Lo que **no** cierra: el audit
  archivado (`docs/archive/sync/AUDIT.md`) lo listaba además como un segundo canal de error sin
  tipar, paralelo a `DomainException` — esa mitad de la deuda sigue abierta y no la toca un rename.
- [ ] **El export saltea los cuatro `LocalDataSource`, y la tabla de capas de `data/CLAUDE.md` todavía
  no lo dice.** Desde la Fase 2a, `DefaultBackupRepository.snapshot()` llama las statements
  directamente, así que el par *qué statement* ↔ *qué cadena de mappers* responde "todas las cuentas"
  existe en dos lugares: `{Entity}LocalDataSource.all()` y `snapshot()`. La atomicidad lo justifica —
  una transacción no puede abarcar data sources basados en Flow — pero si alguien repunta
  `AccountLocalDataSource.all()` a otra statement, el export se queda callado con la vieja. El test de
  tombstones fija el comportamiento actual, no el vínculo. La salida que preservaba la capa era un
  `allForExport()` síncrono por data source; se eligió no meterlo. Riesgo bajo, pero el doc de módulo
  afirma hoy algo que dejó de ser cierto.
- [ ] `SyncOrchestrator` no tiene trigger de reconexión: si un sync falla offline y vuelve la red sin
  escrituras nuevas, no reintenta hasta el próximo `ON_RESUME`. No hay pérdida de data — local-first
  se auto-cura — solo latencia.
- [ ] Pasada de performance de Compose: `derivedStateOf`, lambdas recordadas, `contentType` en
  `LazyColumn`.
- [x] **`BuildInfoConventionPlugin` ya no depende del orden del bloque `plugins { }`.**
  `build-logic/build.gradle.kts` declara el marker de `libs.plugins.android.application` (el tipo de
  variante de AGP entraba solo transitivamente por el marker de KMP-library) y el plugin difiere con
  `pluginManager.withPlugin("com.android.application")`. La fragilidad era real y está medida: con el
  `extensions.configure` eager, subir `id("justchill.build.info")` arriba de
  `alias(libs.plugins.android.application)` falla con *"Extension of type
  'ApplicationAndroidComponentsExtension' does not exist"*; con `withPlugin`, los dos órdenes generan
  `BuildInfo.kt` — los dos se construyeron.
- [ ] La escritura al portapapeles del commit, el split short-en-pantalla/40-al-copiar y la rama
  `SDK_INT < TIRAMISU` del snackbar (`ProfileEntries.kt`) no tienen cobertura automática en ninguna
  tarea del gate. `commitHashUi()` sí la tiene; lo que la rodea, no.
- [x] **El contrato productor/consumidor del commit hash ya no es un string.** `COMMIT_HASH_QUALIFIER`
  se borró; `androidPlatformModule` bindea `CommitHash` (value class sobre `String`) y `AppNavHost`
  lo pide por tipo (`koinInject<CommitHash>().value`). Lo que compra es exactamente una cosa: **no
  queda ningún string que escribir mal**. No hace que los dos lados no puedan discrepar —
  `koinInject<String>()` escrito en esa línea compila verde y revienta al arrancar, igual que antes.
  El mecanismo frena un typo, no un rewrite. `AndroidPlatformModuleTest` pasó de leer los `mappings`
  del propio módulo a **resolver** el tipo contra un `koinApplication { }`, o sea le hace a Koin la
  misma pregunta que `AppNavHost`; borrar el `single` lo pone en rojo con `NoDefinitionFoundException`
  (verificado por mutación). Lo que **no** cambió: sigue sin haber test que observe la línea de
  `AppNavHost` en sí.
- [x] **El contrato de DI salió del paquete de feature de UI.** Vive en
  `presentation/src/androidMain/.../core/CommitHash.kt`, al lado de donde vive el DI del proyecto.
  `:ui-android` lo ve por `api(project(":presentation"))` (`ui-android/build.gradle.kts:38`) y
  `:androidApp` a través de `:ui-android`. Está en `androidMain` y no en commonMain porque productor
  y consumidor son los dos Android-only: así el tipo no entra a la compilación de Kotlin/Native y la
  pregunta por la superficie exportada a iOS no existe.
  `hh/profile/CommitHashUi.kt` queda con una sola razón para cambiar: el estado de presentación del
  footer.
- [ ] `CommitHashUi.Available.fullHash` no lo lee ningún código de producción: `ProfileScreen`
  consume solo `label`, y el camino del portapapeles copia el string inyectado crudo, no el valor
  clasificado. Hoy solo lo miran el `equals` de la data class y `CommitHashUiTest`.
- [ ] La fila del footer de commit (`ProfileScreen.kt`, `CopyableCommitRow`) toma el ripple por
  defecto de Material mientras todas las demás filas interactivas de la pantalla lo apagan con
  `interactionSource` + `indication = null`, y hardcodea `12.sp`/`14.dp` en vez de leer `LocalEmmType`.
- [ ] Flake preexistente en `MviViewModelTest` (~1 de cada 5 corridas, `Dispatchers.Main was
  accessed`). El test ya hace `Dispatchers.setMain(StandardTestDispatcher())` en el `@Before` y
  `resetMain()` en el `@After` (`presentation/src/androidHostTest/.../MviViewModelTest.kt:38` y `:43`),
  así que la fuga es de otro test de la misma JVM, no de este.
- [x] Deps huérfanas en `libs.versions.toml`: **no quedan**. La entrada anterior decía "entre ellas
  `firebase-analytics`, declarada pero sin usar" y eso hoy es falso — el catálogo solo declara
  `firebase-bom` y `firebase-crashlytics`, y las dos se usan en `androidApp/build.gradle.kts:217-218`.
  Los únicos alias que no aparecen en ningún `.gradle.kts` son `detekt-ktlint-wrapper` y
  `detekt-compose-rules`, y entran por `libs.library(...)` desde
  `build-logic/.../DetektConventionPlugin.kt:42-43`.

### Infraestructura

- [ ] Decidir si la protección de `trunk` tiene que alcanzar también a admin. Hoy
  `enforce_admins: false`, así que la cuenta del autor la bypassea y el push directo entra con un
  warning — y en ese push los checks requeridos **no corrieron**.
- [ ] Las dos flojeras del servidor ya descritas arriba: `required_linear_history: false` y
  `allow_force_pushes: true`.

### iOS SwiftUI — track de aprendizaje, sin urgencia

- [ ] S3 a S11: dos de once hechos. Plan y alcance por slice en `docs/swiftui/PLAN.md`, motivación
  en [ADR 005](adr/005-native-swiftui-ios-over-the-kmp-core.md).

### Cerrado y verificado como tal

- [x] Las modales de iOS (export/import/share/email) estaban verificadas solo a nivel de compilación
  y arranque. Ya no cuenta como deuda: el track iOS pasó a SwiftUI nativo por ADR 005 y esas
  pantallas se rehacen en sus slices, no se descongelan.

---

## Track: migración KMP (cerrado)

La app pasó de 3 módulos Android a 4 módulos Kotlin Multiplatform con **una sola
base Compose** para Android e iOS. 67 commits, fast-forward a trunk.

- `:ui-android` (nuevo) tiene toda la UI, los ViewModels y el wiring de Koin.
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

El workflow está en `docs/WORKFLOW.md` (vigente para todo el repo, no solo KMP, desde 2026-08-12);
el ledger de slices con hashes y los landmines carry-forward quedaron en
`docs/archive/kmp/ORCHESTRATION.md`. `MIGRATION_PLAN.md`, `PHASE_2_SPEC.md`, `PHASE_3_SPEC.md` y
`BASELINE.md` también están en `docs/archive/kmp/`: son históricos y los dos specs tienen decisiones
que después se revirtieron.

---

## Track: local-first sync (APAGADO — en rediseño desde el 2026-08-12)

**Apagado en producción** por el kill switch `SYNC_TEMPORARILY_DISABLED` (`SyncKillSwitch.kt`), con
dos gates: `bootstrapAppGraph` en `AppGraph.kt` no llama a `SyncOrchestrator.start()`, y
`ProfileViewModel.syncNow()` corta el path manual antes de encolar nada. No se borró nada — todo
binding, test y clase del motor sigue cableado.

Por qué: el push descartaba toda fila con un `userId` viejo mientras `countPending` las seguía
contando, así que el trigger de escrituras debounceadas re-disparaba cada pocos segundos para
siempre, y los ciclos que sí llegaban a Supabase escribían filas cruzadas entre tenants.

**La decisión que enmarca el rediseño: el sync es BACKUP, no replicación.** Un device a la vez; la
data es del device y la cuenta solo un destino. Retira `ConflictResolver` y el ADR 004 (dormidos, no
borrados), el upsert condicional del server y el arbitraje de relojes, y vuelve **sin objeto** el
pendiente de "convergencia multi-device sin probar" que este doc arrastraba.

Slices 1-4 ✅ shipearon (schema v3, auth opt-in + claim, motor push/pull, lifecycle); slice 5 ⏸. Ojo
con esa lista: "verificado en device" significa **una vez, contra un stack Supabase local**, el
2026-06-10 — el borrado de cuenta pasó esa verificación y después falló en producción sin enviar su
RPC. Todo el detalle en [`docs/archive/sync/AUDIT.md`](archive/sync/AUDIT.md); lo que falta, en el
[checklist](#sync--apagado-en-producción-en-rediseño). Decisiones en `docs/adr/001` y `002`; el plan
de slices original en `docs/archive/sync/PLAN.md`, **cerrado por ADR 009**.

Después de eso, el camino de release es `/release` → tag `vX.Y.Z` → `uploadRelease.yml`. Ese
workflow **no publica**: sube el AAB a la pista alpha como **borrador**, con el `mapping.txt` para
que Play Vitals no reporte frames ofuscados. Un workflow verde no significa que llegó a nadie —
hay que publicar el borrador a mano en Play Console, y recién ahí promover a producción. Se dejó
así a propósito: con `status: completed` un push de tag mandaba el build sin ventana para abortarlo.

---

## Track: auditoría de funcionalidades (cerrada)

Auditoría de lectura sobre `:domain`, las queries `.sq` y los ViewModels clave.
Todo verificado contra el código. Los 4 CRÍTICOS, los 4 ALTOS y los 3 MEDIOS están cerrados.
M11 se cerró con una **decisión de diseño**, no con el fix que pedía la sugerencia original —
el razonamiento está en [ADR 004](adr/004-conflict-resolution-only-arbitrates-unpushed-edits.md)
y más abajo.

Cerrados:

- **C1** Home y Reporte daban totales distintos del mismo mes. `monthlyAmountByCategory`
  usaba `INNER JOIN categories`, así que los movimientos sin categoría no entraban al
  reporte. Ahora `LEFT JOIN` con un bucket "Sin categoría". Arregló de paso
  `GetMonthlyComparisonUseCase` y `GetSavingsRateUseCase`, que pliegan la misma query.
- **C2** Importar respaldo era un `DELETE` físico: los borrados no se propagaban y el
  próximo pull los resucitaba, y encima reventaba por `ON DELETE RESTRICT` en cualquier
  device con un movimiento recurrente. Ahora tombstones + `INSERT OR IGNORE` + `UPDATE`,
  con diálogo de confirmación.
- **C3** Borrar una categoría nuleaba `categoryId` en todo el historial vivo. Ahora solo
  se tombstonea la categoría; el vínculo sobrevive y el export nulea ids colgados.
- **C4** La tasa de ahorro negativa se mostraba como 0%. Se sacó el clamp.
- **A5** Los 24 mensajes de `ValidationError` en inglés llegaban crudos al snackbar porque
  `toUserMessage()` devolvía `message ?: fallback`. Ahora `ValidationError` lleva un
  `ValidationCode`; el `message` queda en inglés para logs y el código es lo que traduce
  `ui-android`. Los 28 call sites están etiquetados.
- **A6** Los recurrentes solo eran pendientes del mes actual, así que un mes sin abrir la app se
  perdía para siempre. `lastConfirmedPeriod` ahora se lee como marca de agua: `pendingPeriods`
  devuelve todos los períodos desde después de la marca hasta hoy, con piso en `createdAt` y tope
  de `MAX_CATCH_UP_MONTHS`. Se confirma del más viejo al más nuevo (guard monótono) y hay un
  "no lo pagué" (`SkipRecurringMovementUseCase`) para no bloquear la cola. La transacción de un
  mes atrasado se fecha en **su** día de vencimiento, no hoy. Sin cambio de schema ni de sync.
- **A8** El promedio mensual dividía siempre entre 6. Ahora divide entre los meses de la ventana
  que tienen movimientos; ingresos y gastos comparten divisor a propósito.
- **M9** El balance de Home plegaba la tabla entera en memoria en cada emisión. Ahora sale de
  `transactions.sq:liveTotals` (balance + conteo en una fila agregada) vía
  `TransactionRepository.observeTotals()`. Los totales del mes siguen plegándose porque la
  pantalla lista esas mismas filas igual. `fetchAllWithCategory()` sigue existiendo para
  Categorías y Ver movimientos, que sí necesitan las filas.
- **M10** El Reporte disparaba ~30 queries suspend secuenciales al abrir (2 ventanas × 6 meses ×
  2 tipos, más 6 de top categorías). Ahora `monthlyAmountByCategoryAndType` agrupa por tipo
  además de por categoría, y `monthlyAmountByCategoryForRanges` corre la ventana entera dentro de
  **una** transacción de lectura: 2 llamadas suspend en total. La transacción no es adorno — sin
  ella los meses se pueden leer a ambos lados de una escritura y el gráfico muestra un estado que
  la base nunca tuvo. Los buckets **no** se calculan en SQL a propósito: el límite entre dos meses
  es hora local y `strftime` sobre epoch daría UTC, que es la misma clase de bug que C1.

- **M11** LWW comparaba relojes de cliente en **todas** las filas del pull, no solo en las que
  tenían conflicto. Un device adelantado ganaba con su propia copia ya sincronizada, la re-pusheaba
  y destruía la edición nueva del otro device, en cada ciclo y en silencio. Ahora
  `ConflictResolver` recibe `LocalRevision(updatedAt, hasUnpushedEdit)`: si la fila local no tiene
  ediciones sin pushear gana el servidor sin mirar ningún reloj. El `updatedAt` de cliente decide
  solo el conflicto genuino de dos lados. Ver [ADR 004](adr/004-conflict-resolution-only-arbitrates-unpushed-edits.md).

La auditoría queda **cerrada**. Lo que la sugerencia original de M11 pedía —cambiar el LWW a
`server_updated_at`— se descartó con evidencia: el schema local no tiene esa columna y, si se
agregara, no responde la pregunta (una edición local sin pushear no tiene timestamp de servidor).
El razonamiento completo está en el ADR 004.

Residuo aceptado a conciencia: si dos devices editan la misma fila antes de sincronizar, el reloj
de cliente sigue desempatando y un device desfasado gana esa carrera. Ahí el costo sí es una sola
edición pisada — el riesgo que ADR 001 aceptó sabiendo lo que aceptaba.

Cuatro afirmaciones de la auditoría **no sobrevivieron a la verificación** — cotejar
contra el código antes de actuar sobre las que quedan:

- **A7** "Borrar cuenta dice 'Delete or move them first' y mover no existe": falso. Mover sí existe
  — `EditTransaction.kt` tiene `AccountPickerSheet` y `TransactionUpdate` lleva `accountId`. El
  único defecto real era el inglés, y se cerró con A5.
- "Importar deja las filas sin reclamar y mata el sync": falso. `observeUnclaimedCount()`
  es un flow reactivo de SQLDelight; el claim corre solo.
- "Después de importar no se dispara sync": falso. El trigger (c) del orquestador observa
  el pending-count con debounce de 3s.
- "Borrar categoría es irreversible **y sin advertencia**": el diálogo ya existía. Lo que
  faltaba era el conteo de movimientos afectados.

---

## Rollback points

`pre-kmp` es el punto de retorno antes de la migración. Tags de release: `v2.2.0`,
`v2.1.0`, `v2.0.0`. Tags de sprint viejos (`pre-s0` … `post-s5`, `pre-redesign`)
siguen en el repo como marcadores históricos.

---

## Mapa de docs

- `docs/WORKFLOW.md` — loop writer/reviewer + gate + tiers de modelo, para todo el repo. **Vigente.**
  `docs/archive/kmp/ORCHESTRATION.md` — el ledger de slices de KMP + landmines. **Cerrado.**
- `docs/work/epics/E01-snapshot-backup.md` — **el único doc vivo de sync**: la decisión de ADR 009
  (el respaldo es snapshot, no replicación) y las constraints que sobreviven a cada ticket. Leerlo
  antes de tocar sync. La auditoría original, el plan de slices viejo y el plan por fases quedan en
  `docs/archive/sync/`, para el *por qué*, no para el *qué sigue*.
- `docs/adr/` — 001 (reversa a local-first con sync opcional), 002 (cursor de pull),
  003 (iOS congelado: se mantiene solo el compile gate, se retira el ritual),
  004 (el resolver de conflictos solo arbitra ediciones sin pushear; enmienda al 002),
  005 (iOS nativo SwiftUI sobre el core KMP; supersede el alcance de UI congelada del 003),
  006 (el sync es backup, un device a la vez; supersede la premisa multi-device del 001 y deja
  dormido al 004), **007** (writer + reviewer separados y tiers de modelo para todo el repo; enmienda
  el punto 5 del 003). Son registros históricos: no se archivan ni se reescriben, se enmiendan con
  otro ADR.
- `docs/DATE_AUDIT.md` — los 13 hallazgos de fechas y qué cerró cada uno. Leer antes de tocar fechas.
- `docs/PLAY_ADVERTISING_ID.md` — la app no usa advertising ID, con los comandos que lo prueban.
- `docs/swiftui/PLAN.md` — las 11 slices del track iOS y su estado.
- `docs/PRODUCT_DISCOVERY.md`, `PRODUCT_REQUIREMENTS.md`, `POST_V1_PLAN.md` — definición de
  producto, Fases 1-5. Siguen vigentes: discovery es la persona y el manifesto, requirements tiene
  los Won't que todavía acotan alcance (y que el ADR 001 enmienda por id de fila), y el post-v1
  es crecimiento sin arrancar.
- `docs/DESIGN_SYSTEM.md` — tokens y componentes. Rutas corregidas el 2026-08-11.
- `docs/PLAY_STORE_LISTING.md`, `docs/PRIVACY_POLICY.md` — material de publicación. El listing
  todavía promete "Sin login. Sin servidor." — lo contradice el ADR 001 y hay que reescribirlo
  antes de subir.
- `docs/archive/` — tracks cerrados que se conservan por el razonamiento. El 2026-08-11 se
  sumaron `ROADMAP_V1.md` (sus 8 sprints se ejecutaron), `AUTH_SYNC_UI_BLUEPRINT.md` (todos sus
  P0 shipearon) y `archive/kmp/` con los cuatro docs de la migración —
  `BASELINE.md`, `MIGRATION_PLAN.md`, `PHASE_2_SPEC.md`, `PHASE_3_SPEC.md`.

---

## Cómo trabajamos acá

- Commits convencionales, en inglés, **sin Co-Authored-By**. Solo los strings de
  UI van en español.
- Historia lineal: siempre `--ff-only`, rebase si divergió, nunca merge commits.
- Nunca pushear sin confirmación explícita en ese momento.
- `./gradlew qualityGate` es el gate. Si tocás una firma de dominio, acordate de que
  el gate incluye `:data:compileAndroidDeviceTest` desde `12ecb2b` — antes de eso los
  tests instrumentados podían quedar rotos con el gate en verde.
- Los tests instrumentados (`:data:connectedAndroidDeviceTest`, **34 tests**) no corren en
  el gate: necesitan device. Corrélos antes de shipear un cambio de schema o de dominio.
  Última corrida: **2026-08-12, 34/34 verde** en `medium_phone` (emulator-5554, API 36), sobre
  `1765e77b` — incluye `MigrationV4ToV5Test` (v4→v5, la FK compuesta) contra un driver real.
  Precedente a tener presente: entre la corrida del 2026-08-09 (15/15) y esta, el conteo creció a 20
  y después a 34 **sin volver a correrlos**, con el gate en verde todo el tiempo. El gate compila
  esta suite, no la ejecuta.
- Writer y reviewer son siempre agentes delegados separados, para **todo el repo** (no solo
  KMP/iOS): el reviewer corre en contexto fresco y nunca escribió el código que revisa —
  [ADR 007](adr/007-one-way-of-working-writer-reviewer-and-model-tiers.md) retira el "un writer,
  review inline" del ADR 003 punto 5, que se apoyaba en una premisa ("no hay usuarios") ya corregida.
  Loop completo y tiers de modelo en `docs/WORKFLOW.md`.
- Los planes de sprint (`PLAN_S*_*.md`) son efímeros y gitignored.
