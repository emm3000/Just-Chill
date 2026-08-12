# JustChill — Progreso

> Punto de re-entrada canónico. Si retomás el proyecto después de un context
> reset, leé esto primero y después el `CLAUDE.md` del módulo que vayas a tocar.
> Si lo único que buscás es qué falta, andá directo al
> [checklist de trabajo abierto](#checklist-de-trabajo-abierto) — es la lista única, y no hay
> ningún `OPEN_WORK.md` compitiendo con ella a propósito.
>
> **Última actualización**: 2026-08-11. No se anota el hash de trunk acá: el commit que lo
> escribe ya lo deja viejo, igual que pasó con el conteo de commits.
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
| Local-first sync (slices 1-5) | slices 1-4 ✅ · slice 5 ⏳ bloqueado por tareas humanas |
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

Medido el 2026-08-11 contra el código, no copiado de la versión anterior de este doc.

### Fechas — lo único abierto que toca el servidor y la data real

- [ ] **Fase dos del hallazgo #5**: el wire de sync y la columna de Supabase todavía llevan
  `date bigint`. `data/shared/FixedPeruOffset.kt` es el adaptador interino que convierte en ambas
  direcciones al offset fijo de Lima, y se borra cuando la columna del server pase a `text`. Los
  trece hallazgos de `docs/DATE_AUDIT.md` están cerrados; este es el único follow-up que sobrevive,
  y es paso humano — cambiar una columna en un server vivo con data acumulada no lo hace el gate.

### Release y compliance — bloqueantes del alpha, solo los puede hacer un humano

- [ ] Crear el proyecto Supabase cloud de prod (`supabase link` + `supabase db push`) y llenar los
  `prod.*` en `supabase.properties`.
- [ ] Hostear `docs/PRIVACY_POLICY.md` como URL pública (Play la exige para apps con eliminación
  de cuenta).
- [ ] Completar el Google Play Data Safety form.
- [ ] Checklist QA multi-device: clean install, semana offline-first, sign-in tardío, dos devices,
  sign-out, y **upgrade real con APK viejo + `adb install -r`**.
- [ ] Corregir la declaración de advertising ID en Play Console: hoy dice "Yes" y es falso. Se puso
  así durante la subida de `v2.4.0` para destrabar un rechazo. `docs/PLAY_ADVERTISING_ID.md` tiene
  la evidencia y los comandos que la reproducen sobre cualquier AAB — leelo antes de flipear.
- [ ] `v2.4.0` está tagueado y construido pero **nunca llegó a la pista alpha**. Un workflow verde
  no publica nada; el borrador se publica a mano en Play Console.

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
  sin cambio antes y después del trabajo del 2026-08-11. Las variantes `prod*` reportan 10: la
  diferencia son nueve archivos que solo existen en `dev`.
- [ ] **El gate no falla con errores de compilación de detekt.** detekt los degrada a warning y la
  tarea termina en `BUILD SUCCESSFUL` — medido. Lo que importa no es el ruido en consola sino que
  cualquier regla que dependa de type resolution puede no dispararse, en silencio.
- [ ] El modo compiler-plugin de detekt sería el arreglo de raíz y hoy no es viable: el plugin id
  `dev.detekt.gradle.compiler-plugin` declara configuration-cache `UNDECLARED` y este build tiene
  `org.gradle.configuration-cache=true` (`gradle.properties:24`). No está aplicado en ningún lado
  del repo, así que esto es una vía cerrada, no una regresión.
- [ ] Purgar las **47** entradas muertas de `UnusedPrivateFunction` en
  `config/detekt/baseline-ui-android-main.xml` (sobre 151 entradas en total). Desde `c94e290` la
  regla ignora los `@Preview` por anotación, así que esas entradas quedaron inertes.
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

- [ ] **Crear una categoría desde un movimiento de Ingreso abre el formulario en Gasto.** Confirmado
  en emulador y contra la base: una categoría de prueba quedó guardada con `categoryType=Spend`
  colgando de una transacción `Income`. Sale de `CategoryRoute.initialType`, que default-ea a
  `CategoryType.Spend` en `ui-android/src/androidMain/kotlin/com/emm/justchill/hh/shared/HhRoutes.kt:72`;
  el push desde el formulario de transacción
  (`hh/transaction/TransactionEntries.kt:46-49`) pasa solo `propagateToTransaction = true` y nunca
  dice de qué tipo es el movimiento que la pidió. El tipo ya lo tiene `AddTransactionViewModel`, así
  que el arreglo es propagarlo en el push — **no** cambiar el default, que rompería los otros dos
  call sites (`CategoryEntries.kt:47` y `AccountEntries.kt:36`) donde `Spend` sí es lo correcto.

### Deuda técnica

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

El workflow, el ledger de slices con hashes y los landmines carry-forward están
en `docs/kmp/ORCHESTRATION.md` — **es el doc vigente y el único que quedó en `docs/kmp/`**.
`MIGRATION_PLAN.md`, `PHASE_2_SPEC.md`, `PHASE_3_SPEC.md` y `BASELINE.md` se archivaron en
`docs/archive/kmp/`: son históricos y los dos specs tienen decisiones que después se revirtieron.

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

Los cuatro viven como checkboxes en el [checklist de trabajo abierto](#release-y-compliance--bloqueantes-del-alpha-solo-los-puede-hacer-un-humano):
proyecto Supabase de prod, hostear la política de privacidad, el Data Safety form y el QA
multi-device. No se repiten acá para que no haya dos listas que se desincronicen.

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

- `docs/kmp/ORCHESTRATION.md` — workflow de slices + ledger. **Vigente.**
- `docs/sync/PLAN.md` — slices de sync + SQL de Supabase.
- `docs/adr/` — 001 (reversa a local-first con sync opcional), 002 (cursor de pull),
  003 (iOS congelado: se mantiene solo el compile gate, se retira el ritual),
  004 (el resolver de conflictos solo arbitra ediciones sin pushear; enmienda al 002).
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
- Los tests instrumentados (`:data:connectedAndroidDeviceTest`, 15 tests) no corren en
  el gate: necesitan device. Corrélos antes de shipear un cambio de schema o de dominio.
  Última corrida: 2026-08-09, 15/15 verde en `medium_phone` (emulator-5554), después de A6.
- Trabajo de KMP / ui-android: **un writer, review inline**. El ritual de writer +
  reviewer como sub-agentes Opus separados por slice se retiró en
  [ADR 003](adr/003-freeze-ios-keep-the-compile-gate.md) — estaba calibrado para
  usuarios en producción que no existen. El gate reforzado de
  `docs/kmp/ORCHESTRATION.md` sigue vigente, compile de iOS incluido.
- Los planes de sprint (`PLAN_S*_*.md`) son efímeros y gitignored.
