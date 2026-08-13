# JustChill — Progreso

> Punto de re-entrada canónico. Si retomás el proyecto después de un context
> reset, leé esto primero y después el `CLAUDE.md` del módulo que vayas a tocar.
> Si lo único que buscás es qué falta, andá directo al
> [checklist de trabajo abierto](#checklist-de-trabajo-abierto) — es la lista única, y no hay
> ningún `OPEN_WORK.md` compitiendo con ella a propósito.
>
> **Última actualización**: 2026-08-12. No se anota el hash de trunk acá: el commit que lo
> escribe ya lo deja viejo, igual que pasó con el conteo de commits.
>
> **El sync está APAGADO en producción desde el 2026-08-12.** Kill switch
> `SYNC_TEMPORARILY_DISABLED` en `presentation/.../core/sync/SyncKillSwitch.kt:16`. La auditoría
> completa —el bug vivo, el forense de producción, el borrado de cuenta que nunca salió del
> teléfono, y la decisión de producto que retira medio diseño— está en
> [`docs/sync/AUDIT.md`](sync/AUDIT.md). Leelo antes de tocar cualquier cosa de sync.
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
| Local-first sync (slices 1-5) | ⏸ **apagado y en rediseño** desde el 2026-08-12 — `docs/sync/AUDIT.md` |
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

### Sync — apagado en producción, en rediseño

Apagado el 2026-08-12 por un loop de sync en producción. **El sync es BACKUP, no replicación: un
device a la vez.** Eso retira `ConflictResolver`, el upsert condicional del server y todo el
arbitraje de conflictos. La causa raíz, el forense de los dos tenants, los ~25 hallazgos vivos y el
plan por fases están en [`docs/sync/AUDIT.md`](sync/AUDIT.md) — acá va solo qué falta, y si esta
lista y el AUDIT se contradicen, gana el AUDIT.

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
- [ ] Fase 0: decidir qué se hace con los dos tenants. AUDIT §8, §10.
- [ ] **Fase 1: decidir el fork de scoping por usuario** — DB por usuario, filtro `userId` en cada
  lectura, o wipe al cambiar de cuenta. Sin decidir. AUDIT §5 (Identity).
- [ ] Fase 1: la app tiene que decir en pantalla que iniciar sesión con otro correo re-apunta el
  backup y sube el ledger de este device ahí.
- [ ] Fase 2 (upsert condicional del server): **despriorizado** por backup-only, no cancelado.
- [ ] Fase 3: rediseño de bordes — tipos en `:domain`, colapsar los cuatro `*TableSync`, cursor a
  SQLDelight. **Restricción vinculante**: cero cambios de `CREATE TABLE`, una sola migración
  aditiva al final. AUDIT §9.
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
- [ ] `toSyncDomainException` mapea `SessionRequiredException` a `NetworkUnavailable`
  (`DefaultSyncRepository.kt:157`), mostrando "Sin conexión" para lo que en el auth path se trata
  como un problema de sesión. Es intencional, no un olvido — el propio comentario en
  `DefaultSyncRepository.kt:151-156` explica por qué (transitorio, no debe cerrar la sesión) — pero
  sigue siendo una divergencia con `toAuthDomainException` (AUDIT §8). Revisar y confirmar que sigue
  siendo la divergencia deseada, no "corregirla" como si fuera un defecto.
- [ ] `toAuthDomainException` mapea `SessionRequiredException` a `Unauthorized` asumiendo que el
  delete path ya está gateado igual que el de sync, pero no lo está: le falta el
  `observeSession.awaitInitialization()` que `DefaultSyncRepository.currentUserId()` sí llama
  (`DefaultSyncRepository.kt:125`) para cerrar la carrera de Kotlin/Native documentada en
  `DefaultSyncRepository.kt:119-124` (postgrest lee el JWT sincrónicamente de un `StateFlow` que se
  llena async). Hoy esa carrera sigue abierta en el delete path y la nueva mapping la muestra como
  error de credenciales en vez de algo reintentable. No se agrega `awaitInitialization()` al delete
  path en este commit — es un cambio de comportamiento, va aparte. `DefaultAuthRepository.kt:206-217`.

### Fechas — lo único abierto que toca el servidor y la data real

- [ ] **Fase dos del hallazgo #5**: el wire de sync y la columna de Supabase todavía llevan
  `date bigint`. `data/src/commonMain/kotlin/com/emm/data/shared/FixedPeruOffset.kt` convierte en
  ambas direcciones al offset fijo de Lima. Los trece hallazgos de `docs/DATE_AUDIT.md` están
  cerrados; este es el único follow-up que sobrevive, y es paso humano — cambiar una columna en un
  server vivo con data acumulada no lo hace el gate.

  **Corregido el 2026-08-12: `FixedPeruOffset.kt` NO se borra cuando la columna pase a `text`.**
  Esta línea decía que sí y es falso — el archivo tiene dos callers (su propia cabecera, `:16-27`) y
  solo uno es el wire de sync; el otro, leer un backup v1 de disco, es **permanente**. La fase dos
  borra un caller, no el archivo. La conversión va con UTC-5, **no** con `AT TIME ZONE 'UTC'`
  (`3.sqm` ya aplicó ese supuesto a la data histórica local): SQL ensayado en verde, 7/7 vectores,
  falta decidir el secuenciamiento. Ver [`docs/sync/AUDIT.md`](sync/AUDIT.md) §5.

### Release y compliance — bloqueantes del alpha, solo los puede hacer un humano

- [x] **El proyecto Supabase cloud de prod ya existe** — corregido el 2026-08-12; esta línea decía
  que faltaba crearlo. Es `pievwpleqmrjwszuuivr` ("Justtt"), linkeado desde el 2026-06-10
  (`supabase/.temp/linked-project.json`), con las tres migraciones aplicadas y los `prod.*`
  poblados en `supabase.properties`. La confusión no era gratuita: ese server tiene dos tenants con
  data real y filas cruzadas. Ver [`docs/sync/AUDIT.md`](sync/AUDIT.md) §7.
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
- [ ] **12 errores de expect/actual**: `:data` (9) y `:presentation` (3). detekt analiza commonMain
  y androidMain como una sola unidad, sin la estructura de fragmentos de HMPP, así que el compilador
  ve el `expect` y su `actual` juntos. Son exactamente **3 errores por par**, no una estimación:
  `:data` tiene tres pares (`shared/Dispatchers.kt`, y dos en `shared/SqliteExceptions.kt`) y
  `:presentation` uno (`core/sync/ResumeEvents.kt`), cada uno con su contraparte `.android.kt`.
- [ ] **13 errores más en `:androidApp:detektDevDebug` y `detektDevRelease`**, sin diagnosticar y
  sin cambio antes y después del trabajo del 2026-08-11. Las variantes `prod*` reportan 10. Los tres
  de diferencia salen del flavor `dev` — probablemente del playground `experiences/`, que solo
  existe ahí — pero eso **no está verificado**: detekt no imprime los mensajes sin `debug = true`
  y con eso tampoco los soltó. Diagnosticar es parte de esta tarea, no un dato ya conocido.
- [ ] **El gate no falla con errores de compilación de detekt.** detekt los degrada a warning y la
  tarea termina en `BUILD SUCCESSFUL` — medido. Lo que importa no es el ruido en consola sino que
  cualquier regla que dependa de type resolution puede no dispararse, en silencio.
- [ ] El modo compiler-plugin de detekt sería el arreglo de raíz —correría dentro de la compilación
  real, con el frontend de verdad, así que los errores de arriba desaparecerían por construcción— y
  hoy no es viable, por dos motivos independientes. **Uno:** el plugin id
  `dev.detekt.gradle.compiler-plugin` declara configuration-cache `UNDECLARED` y este build tiene
  `org.gradle.configuration-cache=true` (`gradle.properties:24`). **Dos:** está roto de fábrica en
  `2.0.0-alpha.6`. El dato no se ve grepeando el repo, hay que sacarlo del jar del plugin
  (`javap` sobre `dev/detekt/detekt_gradle_plugin/BuildConfig.class` en el cache de Gradle):
  `DETEKT_COMPILER_PLUGIN_VERSION = "2.0.0-alpha.6"`, y ese artefacto no existe —
  `dev/detekt/detekt-compiler-plugin/2.0.0-alpha.6/…pom` responde **404** en Maven Central,
  mientras que el publicado de verdad es `2.4.10-2.0.0-alpha.6` (**200**). Aplicarlo tal cual
  falla al resolver `kotlinCompilerPluginClasspath`; haría falta forzar la versión por
  substitución. Vía cerrada, no una regresión.
- [ ] Purgar las **47** entradas muertas de `UnusedPrivateFunction` en
  `config/detekt/baseline-ui-android-main.xml` (sobre 151 entradas en total). Desde `c94e290` la
  regla ignora los `@Preview` por anotación, así que esas entradas quedaron inertes.
- [ ] Entrada `ImportOrdering:ProfileScreen.kt` en `config/detekt/baseline-ui-android-main.xml:37`,
  probablemente muerta desde que `253e170` tocó esos imports. **No verificado**: correr la tarea y
  ver si el issue reaparece antes de borrarla.
- [ ] Sacar el `@Suppress("CyclomaticComplexMethod")` de `ui-android/.../ProfileScreen.kt:239` al
  borrar el kill switch — cubre todo `AccountSection` en vez de solo las ramas de sync. Única
  SUGGESTION del Judgment Day de `253e170`.
- [ ] **Burn-down de los 7 `TooManyFunctions` con amnistía** en
  `config/detekt/baseline-ui-android-main.xml`, contra el umbral de 8 funciones top-level no-`@Preview`
  por archivo: `SeeTransactionsScreen` (16), `HomeScreen` (16), `AddCategoryScreen` (13),
  `AccountsScreen` (11), `AddEditRecurringMovementScreen` (10), `ProfileScreen` (11),
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

### Docs y comentarios que afirman cosas falsas

- [ ] `ui-android/src/androidMain/kotlin/com/emm/justchill/hh/shared/AppNavHost.kt:50-66` — diecisiete
  líneas de cabecera que describen un host que ya no existe. Las tres afirmaciones son falsas:
  no es un "single Compose Multiplatform nav host for both Android and iOS" (`:ui-android` solo
  tiene `androidMain` y `androidHostTest`); no corre sobre el port navigation3-UI de JetBrains
  (`ui-android/build.gradle.kts:50-52` dice que el port "lost its reason to exist" y que runtime y
  UI son de Google); y `PlatformHostActions + startTab` no están detrás de `expect/actual` — son
  una `interface` y un `val` planos en `hh/shared/PlatformHostActions.kt:39` y `:168`. Es código,
  no doc: queda anotado acá y el archivo no se tocó.

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

- [ ] **Editar un movimiento de una categoría borrada lo re-archiva bajo otra, sin que nadie lo elija.**
  `EditTransactionViewModel.resolveSelection` (`:109-112`) corta en `snapshot?.categoryId ?: return null`,
  así que ya no auto-selecciona cuando el movimiento nunca tuvo categoría. Pero la lista viene de
  `categories.sq:all`, que filtra `deletedAt IS NULL`: un movimiento archivado bajo una categoría
  **tombstoneada** tiene `storedId` no-nulo que no está en la lista, y cae en `?: list.firstOrNull()`.
  Abrís un gasto viejo de "Café" (borrada) para corregirle la descripción, el picker muestra "Alquiler"
  seleccionado, `recompute()` ve el cambio, habilita Guardar, y el guardado escribe `categoryId = alquiler`.
  El vínculo que `DeleteCategoryUseCase` preserva **a propósito** (ver su KDoc, y `RecurringMovementFkTest:119-132`)
  se pierde, y el monto queda atribuido a otra categoría en Tendencias.
  Preexistente, no lo introdujo el FK compuesto. El fix del FK adoptó el principio correcto
  ("una etiqueta que nadie eligió") y lo aplicó solo al caso `categoryId == null`; el límite quedó en
  el lado equivocado, porque en los dos casos el usuario no tocó la categoría.
  El otro branch —`changeTransactionType` sobre un movimiento que sí tenía categoría— es defendible:
  ahí el usuario cambió el tipo a propósito.

### Deuda técnica

- [ ] `SyncLogger` (`domain/.../sync/SyncLogger.kt`) está nombrado para el sync path, pero ya es el
  canal general de diagnóstico: lo usan también `DeleteUserAccountUseCase` (borrado de cuenta) y
  `ClaimLocalDataOnAuthenticationUseCase` (claim al autenticarse), ninguno de los dos estrictamente
  sync. `docs/sync/AUDIT.md:115` ya lo lista como un segundo canal de error sin tipar, paralelo a
  `DomainException` — el nombre desalineado es la misma deuda vista desde otro ángulo. Renombrar o
  reubicar el port toca ~18 archivos y es su propia unidad de trabajo, no se hizo acá.
- [ ] `SyncOrchestrator` no tiene trigger de reconexión: si un sync falla offline y vuelve la red sin
  escrituras nuevas, no reintenta hasta el próximo `ON_RESUME`. No hay pérdida de data — local-first
  se auto-cura — solo latencia.
- [ ] Pasada de performance de Compose: `derivedStateOf`, lambdas recordadas, `contentType` en
  `LazyColumn`.
- [ ] Flake preexistente en `MviViewModelTest` (~1 de cada 5 corridas, `Dispatchers.Main was
  accessed`). El test ya hace `Dispatchers.setMain(StandardTestDispatcher())` en el `@Before` y
  `resetMain()` en el `@After` (`presentation/src/androidHostTest/.../MviViewModelTest.kt:38` y `:43`),
  así que la fuga es de otro test de la misma JVM, no de este.
- [x] Deps huérfanas en `libs.versions.toml`: **no quedan**. La entrada anterior decía "entre ellas
  `firebase-analytics`, declarada pero sin usar" y eso hoy es falso — el catálogo solo declara
  `firebase-bom` y `firebase-crashlytics`, y las dos se usan en `androidApp/build.gradle.kts:214-215`.
  Los únicos alias que no aparecen en ningún `.gradle.kts` son `detekt-ktlint-wrapper` y
  `detekt-compose-rules`, y entran por `libs.library(...)` desde
  `build-logic/.../DetektConventionPlugin.kt:41-42`.

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

**Apagado en producción** por kill switch (`SyncKillSwitch.kt:16`), con dos gates: `AppGraph.kt:67`
no llama a `SyncOrchestrator.start()` y `ProfileViewModel.kt:157` corta el path manual. No se borró
nada — todo binding, test y clase del motor sigue cableado.

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
RPC. Todo el detalle en [`docs/sync/AUDIT.md`](sync/AUDIT.md); lo que falta, en el
[checklist](#sync--apagado-en-producción-en-rediseño). Decisiones en `docs/adr/001` y `002`; el plan
de slices original en `docs/sync/PLAN.md`, **pausado**.

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
- `docs/sync/AUDIT.md` — **la auditoría consolidada de sync (2026-08-12)**: causa raíz, forense de
  producción, qué retira el backup-only, forma objetivo y plan por fases. Leerlo antes de tocar sync.
- `docs/sync/PLAN.md` — slices de sync + SQL de Supabase. **Pausado**; slices 1-4 son el registro de
  lo que shipeó, el resto lo reemplaza `AUDIT.md`.
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
