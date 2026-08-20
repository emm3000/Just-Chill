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

### Release y compliance — solo los puede hacer un humano

- [ ] Hostear `docs/PRIVACY_POLICY.md` como URL pública (Play la exige por la eliminación de cuenta).
- [ ] Completar el Google Play Data Safety form.
- [ ] Checklist QA: clean install, semana offline-first, sign-in tardío, sign-out, y upgrade real
      con APK viejo + `adb install -r`.
- [ ] `v2.4.0` nunca llegó a alpha: el workflow falló al commitear el Edit porque el AAB declara
      `AD_ID` y Play Console decía que no. **No hay borrador esperando.** Cerrarlo es cortar una
      release nueva, no publicar a mano.
- [ ] Devolver la declaración de advertising ID a "No". Se puso en "Yes" para desbloquear ese
      rechazo y quedó así. Las dos se destraban juntas: `docs/PLAY_ADVERTISING_ID.md`.

### Fechas

- [ ] Migrar la columna de Supabase de `date bigint` a `text`, UTC-5 (no `AT TIME ZONE 'UTC'`).
      SQL en verde, 7/7 vectores; falta secuenciarlo sobre data real. `docs/archive/sync/AUDIT.md` §5.
- [ ] Detector de drift en CI con el md5 normalizado del schema del server, y mover el guard de
      `relreplident` después del `continue` de idempotencia (`supabase/migrations/`).

### Detekt y baseline

*Acá no se anota el conteo, sino el comando que lo saca: los números driftean.*

- [ ] Purgar las entradas muertas de `UnusedPrivateFunction` en `config/detekt/baseline-ui-android-main.xml`:
      la regla ya ignora los `@Preview` por anotación. `rg -c 'UnusedPrivateFunction' <ese archivo>`.
- [ ] La entrada `ImportOrdering:ProfileScreen.kt` de ese baseline es probablemente muerta.
      Correr la tarea y ver si el issue reaparece antes de borrarla.
- [ ] Burn-down de los `TooManyFunctions` con amnistía en ese baseline: la entrada no lleva conteo,
      así que **el gate no los va a volver a reportar nunca**. Criterio en `docs/CODE_QUALITY.md`.
- [ ] Mismo caso con `LongParameterList` sobre `ProfileRowWithTrailing` (`ProfileScreen.kt`):
      name-keyed y sin conteo, amnistía permanente hasta que se baje.
- [ ] detekt corre **degradado** sobre los pares expect/actual: degrada los errores de compilación a
      warning y termina verde, así que una regla con type resolution puede no dispararse en silencio.
- [ ] El modo compiler-plugin lo arreglaría, pero la coordenada pineada da 404 en Maven Central
      (`dev.detekt:detekt-compiler-plugin:2.0.0-alpha.6`; la publicada es `2.4.10-2.0.0-alpha.6`).
- [ ] `:ui-android:detektAndroidMainSourceSet` y las tareas por flavor de `:androidApp` fallan y están
      fuera del gate a propósito: `DETEKT_GATE_TASKS` es un allowlist literal, no `withType<Detekt>()`.
- [ ] `build-logic` corre en el gate pero no se lintea: su build file aplica solo `kotlin-dsl`.
- [ ] Prohibir `SnackbarHostState.showSnackbar` con `ForbiddenMethodCall` (hoy `active: false` en
      `config/detekt/detekt.yml`). Prenderla arrastra 4 violaciones preexistentes que hay que decidir.

### Seguridad

- [ ] La sesión de Supabase sigue en texto plano en `shared_prefs/justchill_auth.xml`. Cifrarla con
      una clave del Keystore lo cierra; verificá el estado de `androidx.security:security-crypto`.
- [ ] En iOS la sesión va a `NSUserDefaults` (`KoinIos.kt`): sin cifrar y dentro del backup de iCloud.
      El lugar de un refresh token es el Keychain, lo que pide un `SessionManager` propio.
- [ ] Con `allowBackup="false"`, un usuario sin cuenta que pierde el teléfono pierde todo — el backup
      a Supabase solo corre autenticado. Decidir si se avisa o si el modo sin cuenta deja de soportarse.

### Cobertura de tests

- [ ] `HighlightQuotedTest.empty pair produces a bold span over zero characters` promete negrita y
      solo afirma `span.start == span.end`.
- [ ] Nada verifica el binding de `DispatchersProvider` en Koin: vive fuera de `appModules()`.
- [ ] `ReportViewModelTest.rapid month changes are latest-wins…` no verifica cancelación alguna:
      solo afirma `invocationCount >= 1`.
- [ ] Sin cobertura en el gate: la escritura al portapapeles del commit, el split short/40-al-copiar,
      y la rama `SDK_INT < TIRAMISU` del snackbar (`ProfileEntries.kt`).
- [ ] `GenerateBuildInfoTask.generate()` no tiene test; romper las comillas del `trimMargin()` no pone
      nada en rojo. Cerrarlo pide `ProjectBuilder`/`GradleRunner`. Omisión elegida.
- [ ] Flake preexistente en `MviViewModelTest` (`Dispatchers.Main was accessed`): la fuga viene de
      otro test de la misma JVM.

### Docs y copy

- [ ] `docs/DESIGN_SYSTEM.md` §5 documenta 5 radios con otro esquema de nombres; `EmmRadii.kt` ships 9.
      Ningún nombre coincide y dos dp divergen.
- [ ] La app está partida entre tuteo y voseo, y el voseo es el registro equivocado. Vosean
      `PrivacyPolicyScreen`, `ImportBackupDialog`, `DeleteCategoryCopy` y `ProfileMessageText`.
- [ ] La tabla de capas de `data/CLAUDE.md` ya no es cierta: `DefaultBackupRepository.snapshot()`
      llama las statements directamente. La atomicidad lo justifica; el doc afirma lo contrario.

### Deuda técnica

- [ ] La app no tiene ícono adaptativo: el manifest apunta a rasters legacy de 48dp. Rehacerlo pide
      re-autorar el arte a la safe zone de 66dp, y el fuente no está en el repo.
- [ ] Restos del template con nombre visible: `@string/app_name` vale `"Retrofit"` (lo consume el
      shortcut de dev) y el tema se llama `Theme.Retrofit`.
- [ ] Pasada de performance de Compose: `derivedStateOf`, lambdas recordadas, `contentType` en `LazyColumn`.
- [ ] `CommitHashUi.Available.fullHash` es público y solo lo consume su propio `label`.
- [ ] `CopyableCommitRow` (`ProfileScreen.kt`) toma el ripple por defecto mientras las demás filas lo
      apagan, y hardcodea `12.sp`/`14.dp` en vez de leer `LocalEmmType`.

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
