# JustChill — Progreso

> Leé esto primero, y después el `CLAUDE.md` del módulo que vayas a tocar. Acá no se anota
> hash de commit, conteo de commits ni número de línea: el commit que los escribe ya los
> deja viejos.

## Dónde estamos ahora

- App Android de finanzas personales, local-first, en Play Store alpha cerrada.
- No hay usuarios terceros, ni Android ni iOS. Pero **el autor usa la app a diario** sobre la release que sale por Firebase App Distribution, y esa instalación tiene data real acumulada.
- Consecuencia operativa: rehacer navegación, cambiar UI y postergar compliance es barato. Una migración destructiva no. Toda migración de schema **tiene que preservar la data**, y se prueba contra `:data:connectedAndroidDeviceTest` antes de tocar `trunk` — un push a `trunk` distribuye al device del autor.
- El trabajo de backup/sync no se lista acá: vive en `docs/work/`. El índice es `eza docs/work/backlog`.

## Cómo se verifica el estado

```bash
git rev-list --count origin/trunk..trunk            # commits sin pushear
git rev-list --count --merges origin/trunk..trunk   # debe dar 0: la historia es lineal
gh api repos/emm3000/Just-Chill/branches/trunk/protection --jq '.required_status_checks.contexts'
eza docs/work/backlog                                # el trabajo con ticket
```

Nunca mergear sin `--ff-only`. `trunk` exige PR + status checks (`quality-gate`,
`ios-compile`), pero admin los bypassea; en un push directo esos checks **no corrieron** y
la única verificación es la local.

## Trabajo abierto

### Release y compliance — solo los puede hacer un humano

- [ ] Hostear `docs/PRIVACY_POLICY.md` como URL pública (Play la exige por la eliminación de cuenta).
- [ ] Completar el Google Play Data Safety form.
- [ ] Checklist QA: clean install, semana offline-first, sign-in tardío, sign-out y upgrade real con
      APK viejo + `adb install -r`. Multi-device quedó sin objeto: la decisión de backup-only es un
      device a la vez.
- [ ] Corregir la declaración de advertising ID en Play Console: hoy dice "Yes" y es falso.
      Evidencia y comandos: `docs/PLAY_ADVERTISING_ID.md`. Leelo antes de flipearlo.
- [ ] `v2.4.0` está tagueado y construido pero nunca llegó a la pista alpha. Un workflow verde no
      publica: el borrador se publica a mano en Play Console.
- [ ] Reescribir `docs/PLAY_STORE_LISTING.md`. **Ojo, la razón cambió con ADR 009**: la ficha promete
      "Sin login. Sin servidor.", "No te sincroniza con la nube" y "sin sync". Lo de *sync* ya es
      cierto — el motor se borró. Lo que sigue siendo falso es "sin login" y "sin servidor": el
      backup a Supabase usa cuenta y storage. Publicar eso tal cual declara algo falso al lado del
      Data Safety form.

### Fechas

- [ ] Fase dos del hallazgo #5: migrar la columna de Supabase de `date bigint` a `text`, UTC-5 (no
      `AT TIME ZONE 'UTC'`). SQL en verde, 7/7 vectores; falta el secuenciamiento humano sobre data
      real. Detalle: `docs/archive/sync/AUDIT.md` §5.
- [ ] Detector de drift en CI con el md5 normalizado del schema del server, y mover el guard de
      `relreplident` después del `continue` de idempotencia en la migración de composite primary
      keys (`supabase/migrations/`).

### Detekt y baseline

*Acá no se anota el conteo, sino el comando que lo saca: estos números ya driftearon dos veces
(dev/prod 13/10 → 15/12; `androidMainSourceSet` 21 → 22).*

- [ ] Purgar las entradas muertas de `UnusedPrivateFunction` en
      `config/detekt/baseline-ui-android-main.xml`: desde que la regla ignora los `@Preview` por
      anotación quedaron inertes. Contalas con
      `rg -c 'UnusedPrivateFunction' config/detekt/baseline-ui-android-main.xml`.
- [ ] La entrada `ImportOrdering:ProfileScreen.kt` de ese mismo baseline es probablemente muerta.
      Correr la tarea y ver si el issue reaparece antes de borrarla.
- [ ] Burn-down de los `TooManyFunctions` con amnistía en ese baseline, contra el umbral de 8
      funciones top-level no-`@Preview` por archivo: `SeeTransactionsScreen`, `HomeScreen`,
      `AddCategoryScreen`, `AccountsScreen`, `AddEditRecurringMovementScreen`, `ProfileScreen`,
      `RecurringMovementsScreen`. La entrada del baseline no lleva conteo, así que **el gate no los
      va a volver a reportar nunca**, crezcan lo que crezcan. Criterio en `docs/CODE_QUALITY.md`.
- [ ] Mismo caso con `LongParameterList` sobre `ProfileRowWithTrailing` (`ProfileScreen.kt`): 8
      parámetros contra el tope de 5, name-keyed y sin conteo, amnistía permanente hasta que se baje.
- [ ] detekt corre **degradado** sobre los pares expect/actual: analiza commonMain y androidMain como
      una sola unidad, sin fragmentos HMPP, y cuenta 3 errores de compilación por par en `:data` y
      `:presentation`. Un `detektMainAndroid` verde no prueba lint completo, y el gate **no falla**
      por eso — detekt degrada esos errores a warning y termina en `BUILD SUCCESSFUL`. Cualquier
      regla que dependa de type resolution puede no dispararse en silencio.
- [ ] El modo compiler-plugin lo arreglaría de raíz, pero la coordenada pineada no existe:
      `dev.detekt:detekt-compiler-plugin:2.0.0-alpha.6` da 404 en Maven Central; la publicada es
      `2.4.10-2.0.0-alpha.6`. Queda sin medir el choque con `org.gradle.configuration-cache=true`:
      el plugin no está aplicado en ningún módulo, así que no se puede reproducir sin aplicarlo.
- [ ] `:ui-android:detektAndroidMainSourceSet` falla, y está deliberadamente fuera del gate:
      `detektMainAndroid` cubre los mismos archivos **con** type resolution. Mismo caso con las
      tareas por flavor de `:androidApp`. `DETEKT_GATE_TASKS` en `QualityGateConventionPlugin` es un
      allowlist literal a propósito, no `tasks.withType<Detekt>()`.
- [ ] `build-logic` corre en el gate pero no se lintea: su build file aplica solo `kotlin-dsl`, y la
      entrada de detekt ahí es un marker `implementation` para poder escribir `DetektConventionPlugin`.
- [ ] Prohibir `SnackbarHostState.showSnackbar` con `ForbiddenMethodCall` (`config/detekt/detekt.yml`,
      hoy `active: false`), para que un error pintado de verde no vuelva a mergear — el gate corre
      `detektMainAndroid` sobre `:ui-android` con type resolution, así que la regla funcionaría. Costo
      ya medido: prenderla activa también sus otras entradas, y eso son cuatro violaciones
      preexistentes — el `println` de `PrintlnDiagnosticsLogger` (la clase se llama así a propósito) y
      tres `BigDecimal(String)` en `EmmAmountChill.kt`. Decidir cada una es parte del trabajo.

### Bugs

- [ ] Cambiar el tipo de un movimiento le elige categoría sin que nadie lo pida:
      `EditTransactionViewModel.resolveSelection` cae en `?: list.firstOrNull()`, así que el botón de
      guardar se habilita con una categoría arbitraria. Su propio KDoc explica por qué no cae cuando
      no hay categoría guardada, pero sí cae cuando hay una. El load inicial no tiene el defecto:
      resuelve a null y deja el movimiento sin categoría, que es lo correcto.
- [ ] La pantalla de Reportes se traga todos sus errores: `ReportViewModel` emite
      `ReportEffect.ShowError`, pero `ReportScreen` no recibe `SnackbarHostState` y la rama queda en
      `-> Unit`. Un fallo de carga o de compartir no le dice nada al usuario.
- [ ] `TRENDS_WINDOW_MONTHS = 6` (`ReportViewModel.kt`) está repetido como texto literal "6 meses"
      en `ReportShareFormatter.kt`. Cambiar la constante deja el texto compartido mintiendo.
- [ ] `DefaultAuthRepository.deleteAccount()` dispara el RPC sin `awaitSessionInitialization()`, así
      que en la ventana de restore de sesión el fallo sale como error de credenciales en vez de
      reintentable. Agregar el await es un cambio de comportamiento aparte.

### Seguridad

- [ ] La sesión de Supabase sigue en texto plano en `shared_prefs/justchill_auth.xml`. Ya está fuera
      de Auto Backup y de device-transfer, pero un device rooteado o una extracción física la leen.
      Cifrarla con una clave del Android Keystore (que no sale del device) lo cierra.
      `androidx.security:security-crypto` es el camino obvio, pero **verificá su estado antes de
      adoptarlo** — estuvo deprecado.
- [ ] En iOS la sesión va a `NSUserDefaults` (`KoinIos.kt`), que es el equivalente de
      SharedPreferences: sin cifrar y dentro del backup de iCloud. En iOS el lugar de un refresh
      token es el Keychain. Pide un `SessionManager` propio, no un `SettingsSessionManager`.
- [ ] Con `allowBackup="false"` un usuario sin cuenta que pierde el teléfono pierde todo: el backup
      automático a Supabase solo corre bajo `SessionStatus.Authenticated` (`BackupOrchestrator`
      dispara en cada background/resume), y ya no hay respaldo de Android que lo cubra. Decidir si
      eso se avisa en la app o si el modo sin cuenta deja de ser un modo soportado.

### Cobertura de tests

- [ ] `HighlightQuotedTest.empty pair produces a bold span over zero characters` solo afirma
      `span.start == span.end`; sus tres hermanos sí chequean `FontWeight.W700`. El nombre promete
      negrita y la aserción no la mira.
- [ ] Nada verifica el binding de `DispatchersProvider` en Koin: vive fuera de `appModules()`, así que
      `AppGraphKoinTest` no lo ve, y `AndroidPlatformModuleTest` solo resuelve `CommitHash`.
- [ ] `ReportViewModelTest.rapid month changes are latest-wins — first load is cancelled by the second`
      no verifica ninguna cancelación: solo afirma `invocationCount >= 1`.
- [ ] Sin cobertura en ninguna tarea del gate: la escritura al portapapeles del commit, el split
      short-en-pantalla/40-al-copiar, y la rama `SDK_INT < TIRAMISU` del snackbar (`ProfileEntries.kt`).
- [ ] El template de `GenerateBuildInfoTask.generate()` no tiene test: `normalizeCommitHash` sí, pero
      romper las comillas del `trimMargin()` no pone en rojo nada dentro de `build-logic`. Cerrarlo
      pide `ProjectBuilder`/`GradleRunner` contra un directorio temporal. Omisión elegida.
- [ ] Flake preexistente en `MviViewModelTest` (`Dispatchers.Main was accessed`). El test ya hace
      `setMain`/`resetMain`, así que la fuga es de otro test de la misma JVM.

### Docs y copy

- [ ] `docs/DESIGN_SYSTEM.md` §5 documenta 5 radios con otro esquema de nombres; `EmmRadii.kt` ships
      9. Ningún nombre coincide, y dos dp divergen: `radius.s` 6 contra `rS` 10, y `radius.l` 20
      contra `rL` 14 — los 20dp del doc son los de `rXXL`. `rXS`, el que usa el footer de commit, no
      tiene fila.
- [ ] La app está partida entre tuteo y voseo, y el voseo es el registro equivocado para un usuario
      peruano. Vosean `PrivacyPolicyScreen.kt`, `ImportBackupDialog.kt`, `DeleteCategoryCopy.kt` (dos
      cadenas) y `ProfileMessageText.kt`; el resto tutea. Nada lo detecta: detekt no lee español.
      Decidir el registro una vez y barrer.
- [ ] La tabla de capas de `data/CLAUDE.md` ya no es cierta: `DefaultBackupRepository.snapshot()`
      llama las statements directamente en vez de pasar por los cuatro `LocalDataSource`. La
      atomicidad lo justifica (una transacción no abarca data sources basados en Flow), pero el doc
      afirma lo contrario.

### Deuda técnica

- [ ] La app no tiene ícono adaptativo: el manifest apunta a los rasters legacy de 48dp y con
      `minSdk 28` todo device recibe el tratamiento legacy. Las piezas del generador se borraron
      porque el retrato ocupaba los 108dp enteros y la máscara le cortaba la cabeza; rehacerlo pide
      re-autorar el arte a la safe zone de 66dp y el fuente no está en el repo.
- [ ] Restos del template "Retrofit" con nombre visible: `@string/app_name` vale `"Retrofit"` y lo
      consume el shortcut de dev (por eso `strings.xml` vive en `src/dev/`), y el tema de la app se
      llama `Theme.Retrofit`. El label de la app no sale de ahí — el manifest usa `${app_name}` de
      manifestPlaceholders.
- [ ] Pasada de performance de Compose: `derivedStateOf`, lambdas recordadas, `contentType` en
      `LazyColumn`.
- [ ] `CommitHashUi.Available.fullHash` es público y solo lo consume su propio `label`; ningún otro
      código de producción lo lee — el camino del portapapeles copia el string crudo.
- [ ] `CopyableCommitRow` (`ProfileScreen.kt`) toma el ripple por defecto de Material mientras las
      demás filas interactivas de la pantalla lo apagan con `interactionSource` + `indication = null`,
      y hardcodea `12.sp`/`14.dp` en vez de leer `LocalEmmType`.

### Infraestructura

- [ ] Subir el wrapper de Gradle 9.7.0 → 9.7.1. Es el único warning que queda en
      `:androidApp:lintProdRelease` (`AndroidGradlePluginVersion`) y se deja visible a propósito:
      dependabot parsea `libs.versions.toml`, no `gradle-wrapper.properties`, así que nada más lo
      reporta. Va en su propio commit con el gate corrido.
- [ ] Decidir si la protección de `trunk` alcanza a admin. Hoy `enforce_admins: false`: la cuenta del
      autor bypassea y en ese push los checks requeridos no corrieron.
- [ ] `required_linear_history: false` y `allow_force_pushes: true`, ambas decididas a conciencia y
      todavía sin tocar. Cambiarlas exige un `PUT` del objeto de protección completo, no un PATCH.

## Rollback points

`pre-kmp` es el punto de retorno antes de la migración KMP. Tags de release: `v2.4.0`, `v2.3.0`,
`v2.2.0`, `v2.1.0`, `v2.0.0`, `v1.6.0`. Los tags de sprint viejos (`pre-s0` … `post-s5`,
`pre-redesign`) siguen en el repo como marcadores históricos.
