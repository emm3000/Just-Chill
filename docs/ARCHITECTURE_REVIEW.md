# ARCHITECTURE REVIEW — JustChill v1

**Fase**: 3 de 5 (Architecture / Gap Analysis)
**Bloquea**: ROADMAP_V1.md (Fase 4).
**Depende de**: PRODUCT_DISCOVERY.md (§1, firmada) + PRODUCT_REQUIREMENTS.md
(§2, firmada).

> Este doc contrasta el estado actual del código contra cada Must / Should
> de Fase 2. Su único propósito es darle a Fase 4 una lista priorizada
> de "qué falta, cuánto cuesta, en qué orden". No diseña features —
> nombra gaps.

---

## 0. Resumen ejecutivo (lo que tenés que saber en 60 segundos)

**La buena**: la base está sana. Arquitectura limpia (3 módulos, dependencia
unidireccional, MVI consistente, DomainException + SafeCall ya migrado).
**Las features de captura, edición, búsqueda y gestión de categorías/cuentas
están listas** y al nivel de calidad de v1.

**La mala**: **la apuesta diferencial no existe en código**. El reporte de
ingresos por fuente (US-11) — la pieza que justifica la app — está en 0%.
Y hay 4 Musts más en 0%: onboarding/manifiesto (US-01/02), export/import
(US-18/19), navegación entre meses (US-09), cuentas preset peruanas (US-03).

**Lo que pasa si arrancamos Fase 4 hoy**: 8 de 15 Musts pendientes. ~1,600
LOC mínimas. A ritmo de 5-6 h/semana: **1.5-2 semanas de trabajo real**
para llegar a alpha en Play Store.

**Lo único bloqueante crítico**: US-11. Sin reporte de ingresos por fuente,
v1 es "otra app de gastos más". Con él, es JustChill.

---

## 1. Tabla de estado por story

> Estados: ✅ **IMPL** = implementado y al nivel de v1.
> 🟡 **PARCIAL** = existe parcialmente, falta trabajo concreto.
> ❌ **NO** = no existe.
> ⚠️ **REHACER** = existe pero contradice algún requirement de Fase 2.

| Story | Prio | Estado | Path principal | Qué falta |
|---|---|---|---|---|
| US-01 | M | ❌ | `MainActivity.kt` → `Hh()` directo | Sin first-launch detection. |
| US-02 | M | ❌ | — | Sin pantalla de manifiesto. |
| US-03 | S | 🟡 | `providers.kt` seedDefaultCategories | Faltan **2 categorías ingreso** (Ventas, Otros) + **7 cuentas preset** (Efectivo, Yape, Plin, BCP, BBVA, Interbank, Scotiabank). |
| US-04 | M | ✅ | `AddTransactionScreen.kt` | — |
| US-05 | M | ✅ | `AddTransactionScreen.kt` toggle | — |
| US-06 | M | ✅ | `EditTransaction.kt` | — |
| US-07 | S | ✅ | `SeeTransactionsScreen.kt` + `SearchTransactionsUseCase` | — |
| US-08 | M | 🟡 | `HomeScreen.kt` | Falta US-09 acoplado. |
| US-09 | M | ❌ | `HomeViewModel.kt` mes hardcode actual | Sin picker/swipe/flechas. |
| US-10 | S | ✅ | `HomeScreen.kt` `BalanceHero` | — |
| US-11 | **M** | ❌ | — | **THE APUESTA. 0% done.** Sin use case, sin SQL, sin Screen. |
| US-12 | S | ❌ | — | Derivado de US-11. |
| US-13 | C | ❌ | — | v2. |
| US-14 | M | ✅ | `AddCategoryScreen.kt` + family | — |
| US-15 | M | ✅ | `AccountsScreen.kt` + family | — |
| US-16 | M | 🟡 | `formatExpense()`, `formatIncome()` | Formateadores usan `S/` pero hay que **verificar default currency en schema** (sospecha: queda algún `ARS` legacy). |
| US-17 | S | ❌ | — | Cubierto por US-03 (no hay seed de cuentas). |
| US-18 | M | ❌ | `ProfileScreen` botón dummy (`onClick = {}`) | Sin serializers, sin use case, sin file I/O. |
| US-19 | M | ❌ | `ProfileScreen` botón dummy | Sin file picker, sin import logic. |
| US-20 | C | ❌ | — | v2. |
| US-21 | M | 🟡 | `ProfileScreen.kt` (skeleton ~25%) | 2/7 ítems funcionando. Faltan: Categorías/Cuentas (wiring), Exportar/Importar (lógica), Acerca de, Política, Versión. |

**Resumen numérico**:

| Estado | Cuántas |
|---|---|
| ✅ IMPL | 7 (US-04, 05, 06, 07, 10, 14, 15) |
| 🟡 PARCIAL | 4 (US-03, 08, 16, 21) |
| ❌ NO | 9 (US-01, 02, 09, 11, 12, 13, 17, 18, 19, 20) — 5 de ellas son Must |
| ⚠️ REHACER | 0 |

---

## 2. Legacy — qué hay que matar

**Hallazgo importante**: `CLAUDE.md` dice que quedan tablas legacy
(`drivers`, `dailies`, `loans`, `payments`). **No es verdad**. Esas tablas
ya no existen en `data/src/main/sqldelight/`. La migración local-only las
borró. **Hay que actualizar `CLAUDE.md` para reflejarlo.**

**Lo que sí queda como legacy real**:

| Item | Dónde | Acción | Riesgo |
|---|---|---|---|
| Botón "Cerrar sesión" en `ProfileScreen` | `ProfileScreen.kt` | Borrar. La app no tiene sesión desde la migración local-only. | 0 |
| Botón "Cuenta por defecto" en `ProfileScreen` | `ProfileScreen.kt` | Validar si tiene sentido en v1 (si la app tiene múltiples cuentas, sí; revisar UX). | Bajo |
| Permission `INTERNET` en `AndroidManifest.xml` (línea 5) | `app/src/main/AndroidManifest.xml` | Borrar. Fase 2 §6 lo prohíbe. | 0 (app es 100% local) |
| Deps `androidx.constraintlayout` | `app/build.gradle.kts` | Borrar. No se usa en Compose. | 0 |
| Deps `androidx.navigation.fragment.ktx`, `androidx.navigation.ui.ktx`, `androidx.lifecycle.livedata.ktx` | `app/build.gradle.kts` | Borrar. Navigation3 es el estándar; LiveData no se usa. | Bajo |
| `coil` (si no se usa hoy) | `app/build.gradle.kts` | Validar uso. Si nadie lo importa, borrar. | Bajo |
| Currency default `ARS` | `accounts.sq` o seed | Cambiar a `PEN` o quitar el campo (v1 es solo soles, no hay multi-moneda). | **Medio — puede romper data existente en dev** |

**Estimación**: ~40 líneas borradas, ~50-60 KB menos en el APK, 0
funcionalidad perdida.

---

## 3. Gaps críticos (priorizados por impacto)

### 3.1 US-11 — Reporte de ingresos por fuente (la apuesta)

**Estado**: 0%. No hay use case, no hay SQL, no hay Screen, no hay
ViewModel, no hay navegación.

**Por qué es el #1**: es la única feature que diferencia JustChill de
Monefy/Spendee. Sin esto, lo que estamos por publicar es "otra app de
gastos más con UI minimalista".

**Lo que hay que construir** (esto NO es diseño, es inventario para Fase 4):

- **`:domain`**:
  - `GetMonthlyIncomeByCategoryUseCase` → devuelve `List<CategoryShare>`
    (categoría + total + porcentaje del mes).
  - `GetMonthlyComparisonUseCase` (mes actual vs anterior).
- **`:data`**:
  - Nueva query en `transactions.sq`:
    `SUM(amount) AS total, categoryId GROUP BY categoryId WHERE type='Income' AND date BETWEEN ? AND ?`.
  - Adapter en `TransactionLocalDataSource` + repositorio.
- **`:app`**:
  - `ReportScreen.kt` + `ReportViewModel` + `ReportUiState` (MVI).
  - Componente visual: barras o donut. **Decisión de diseño pendiente
    para Fase 4** (donut es más "screenshoteable" pero barras son más
    legibles).
  - Navegación: tab nueva o botón desde Home.
  - Texto grande, alto contraste (requisito de Fase 1 §5: "tiene que
    verse bien en zoom").
- **Tests**:
  - Use case con MockK + `runTest`.
  - Verificar % suma 100, edge cases (mes sin ingresos, una sola
    categoría).

**Estimación**: 600-800 LOC, 2-3 días.

---

### 3.2 US-18 / US-19 — Export / Import JSON

**Estado**: skeleton dummy. Botones en `ProfileScreen.kt` con
`onClick = {}`.

**Por qué importa**: es la promesa de "tu data es tuya" hecha verbo.
Sin export, todo el manifiesto suena hueco.

**Lo que hay que construir**:

- **`:domain`**:
  - `ExportDataUseCase` → devuelve `ExportPayload` (DTO con listas).
  - `ImportDataUseCase` → recibe `ExportPayload`, valida, **reemplaza**
    (v1 simple, no merge — Fase 2 US-19 ya lo firmó).
  - Modelo `ExportPayload` con `schemaVersion: Int` para futuras
    migraciones.
- **`:data`**:
  - Serializers: `TransactionDto`, `CategoryDto`, `AccountDto` con
    `kotlinx-serialization` (ya está en classpath si Kotlin lo trae;
    si no, agregar).
  - Wrappers de file I/O en `:data/shared/` para no contaminar `:app`.
- **`:app`**:
  - `ProfileViewModel`: lógica de export/import + estado MVI.
  - `LaunchedEffect` con `ActivityResultContract` para file picker /
    save-document intent.
  - Diálogos de confirmación con texto brutal: *"Esto va a borrar lo
    que tengas hoy y poner lo del archivo."*
- **Tests**:
  - Roundtrip: export → import → comparar igualdad de data.
  - Corruption: JSON malformado, JSON con `schemaVersion` distinto.
  - Vacío: importar payload vacío.

**Decisión arquitectónica pendiente para Fase 4**: ¿agregamos
`kotlinx-serialization` como dep o usamos org.json (más feo pero ya
viene en Android)? Recomendación: kotlinx-serialization (más limpio,
typesafe, ~150 KB extra OK).

**Estimación**: 400-600 LOC, 2 días.

---

### 3.3 US-01 / US-02 — Onboarding + manifiesto visible

**Estado**: 0%. `MainActivity.kt` carga `Hh()` directo.

**Por qué importa**: Fase 1 §3.5 dice "el manifiesto es la apuesta de
marca". Si Sebastián instala y abre, lo primero que ve es una pantalla
en blanco. No hay diferenciación, no hay "ah, esta app cree algo".

**Lo que hay que construir**:

- **Detección de primer launch**:
  - DataStore Preferences (nuevo) con clave `"first_launch_seen"`.
  - DataStore es la opción Compose-friendly (vs SharedPreferences).
  - 1 archivo nuevo en `:app/core/preferences/` para encapsular.
- **`FirstLaunchScreen.kt`**:
  - Compose, layout simple, fondo limpio.
  - Render del manifiesto de Fase 1 §3.5 (texto exacto).
  - Botón único: "Empezar".
- **Navegación**:
  - En `MainActivity.kt`, decidir basado en flag DataStore:
    `FirstLaunch → Hh` o `Hh` directo.
- **Acceso desde ajustes** (US-21): link "Acerca de" reabre la misma
  pantalla.

**Estimación**: 200-300 LOC, 1 día.

---

### 3.4 US-09 — Navegación entre meses en Home

**Estado**: 0%. `HomeViewModel` consulta mes actual hardcode.

**Por qué importa**: sin esto, Sebastián no puede comparar con el mes
pasado, no puede ver tendencia, y el reporte de US-11 queda atrapado al
mes actual.

**Lo que hay que construir**:

- **`HomeUiState`**: agregar `month: YearMonth` (kotlinx-datetime).
- **`HomeIntent`**: `PreviousMonth`, `NextMonth`, `JumpToToday`.
- **`HomeViewModel`**: estado mutable de `currentMonth`, lógica para
  calcular `startInclusive` / `endExclusive` y recombinar el Flow del
  repo. SQL no cambia (ya existe `completeTransactionsByDateRange`).
- **`HomeScreen`**: chevrons izq/der + indicador del mes (`"Mayo 2025"`)
  + tap-to-today si no está en el mes actual.

**Estimación**: 150-200 LOC, 0.5-1 día.

---

### 3.5 US-03 — Cuentas y categorías preset peruanas

**Estado**: parcial. Hay seed de categorías pero faltan 2 ingresos
(Ventas, Otros) y **todo el seed de cuentas**.

**Por qué importa**: es la peruanidad concreta. Si Sebastián abre la app
y la primera cuenta que crea es manualmente "Yape", la peruanidad NO
se demuestra — se delega al usuario.

**Lo que hay que construir**:

- **`providers.kt`**:
  - `seedDefaultAccounts()` paralelo a `seedDefaultCategories()`.
  - INSERT 7 cuentas: Efectivo (tipo cash), Yape (digital wallet), Plin
    (digital wallet), BCP (bank), BBVA (bank), Interbank (bank),
    Scotiabank (bank).
  - Llamar desde `onCreate()` del callback de SQLDelight (junto con
    categorías).
- **Seed categorías**:
  - Agregar 2 ingresos: `Ventas`, `Otros` (Sueldo / Freelance / Inversiones
    ya están; Propinas existe en `IconsAll.kt` pero no en seed).
- **Decisión pendiente para Fase 4**: ¿íconos/colores característicos
  de cada banco? Posible problema legal con marcas. Recomendación:
  íconos genéricos en v1.

**Estimación**: ~30 líneas. ½ día.

---

## 4. Quick wins (alta razón cambio/riesgo)

Cosas que se hacen en <1 día total, sin riesgo, y que reducen ruido del
proyecto. **Hacerlas todas antes de Fase 4** para que el roadmap arranque
limpio.

| # | Acción | Archivo | Tiempo | Riesgo |
|---|---|---|---|---|
| QW-1 | Borrar `INTERNET` permission | `AndroidManifest.xml:5` | 1 min | 0 |
| QW-2 | Borrar dep `androidx.constraintlayout` | `app/build.gradle.kts:114` | 5 min | 0 |
| QW-3 | Borrar deps de Navigation legacy (`fragment.ktx`, `ui.ktx`, `livedata.ktx`) | `app/build.gradle.kts:134-136` | 5 min | Bajo |
| QW-4 | Borrar botón "Cerrar sesión" de `ProfileScreen` | `ProfileScreen.kt` | 5 min | 0 |
| QW-5 | Actualizar `CLAUDE.md` (sacar mención de tablas legacy que ya no existen) | `CLAUDE.md` | 5 min | 0 |
| QW-6 | Cambiar default currency `ARS → PEN` (o eliminar el campo) | `accounts.sq` + migraciones | 20-30 min | Medio — destruye data dev. Aceptable pre-alpha. |
| QW-7 | Agregar 2 categorías ingreso al seed (Ventas, Otros) | `providers.kt` seedDefaultCategories | 5 min | 0 |
| QW-8 | Wiring de "Categorías" y "Cuentas" en `ProfileScreen` (links a screens existentes) | `ProfileScreen.kt` + `HhRoutes.kt` | 20 min | 0 |
| QW-9 | Mostrar versión de la app en `ProfileScreen` (BuildConfig.VERSION_NAME) | `ProfileScreen.kt` | 10 min | 0 |

**Total**: ~80 min, ~50-60 KB menos de APK, mucho menos ruido en el código.

---

## 5. Big rocks (lo que mueve la aguja)

| # | Story | Esfuerzo | LOC aprox | Bloqueante para v1 |
|---|---|---|---|---|
| BR-1 | US-11 Reporte ingresos | 2-3 días | 600-800 | **SÍ — la apuesta** |
| BR-2 | US-18/19 Export/Import JSON | 2 días | 400-600 | SÍ |
| BR-3 | US-01/02 Onboarding + manifiesto | 1 día | 200-300 | SÍ |
| BR-4 | US-09 Navegación meses | 0.5-1 día | 150-200 | SÍ |
| BR-5 | US-21 ProfileScreen completo (Acerca de + Política + Versión + wiring) | 0.5 día | 100-150 | SÍ |

**Total Big Rocks**: ~6-8 días de trabajo concentrado, ~1,450-2,050 LOC.

A ritmo realista de **5-6 h/semana**, eso son ~3 semanas. Sumando 1
semana de polish + dogfooding antes de alpha en Play Store: **~4 semanas
totales para v1 alpha**.

---

## 6. Lectura arquitectónica (no son features, son patrones)

### 6.1 Lo que la arquitectura aguanta bien
- **MVI base + `launchSafe`**: agregar nuevas features (Reporte, Export)
  encaja sin fricción. `MviViewModel<S, I, E>` ya es el contrato.
- **`SafeCall` + `DomainException`**: el patrón de error está cubierto.
  Las nuevas use cases (Reporte, Export, Import) van a usarlo sin
  cambiar nada.
- **3 módulos**: el reporte vive limpio en `:domain` + `:data`, sin
  contaminar `:app`. Lo mismo Export/Import.
- **SQLDelight**: agregar `summaryByCategory` es una query más. La
  perf está controlada (índices en `categoryId` y `date` deberían
  validarse — Fase 4 lo audita).

### 6.2 Lo que la arquitectura NO está preparada para
- **DataStore Preferences**: no existe. Para US-01 (first-launch flag)
  hay que introducirlo. Es estándar Android y no rompe nada, pero es
  una dep nueva.
- **File I/O para Export/Import**: hoy no hay nada. Hay que crear
  wrappers en `:data/shared/` (o `:app/core/io/` — decisión Fase 4).
- **Navegación condicional inicial**: hoy `MainActivity` carga `Hh()`
  directo. Para US-01 hay que agregar una capa que decida `FirstLaunch`
  o `Hh` antes de mostrar nada. Fácil pero hay que diseñarlo.

### 6.3 Riesgos arquitectónicos a vigilar
- **Acoplamiento HomeScreen ↔ Reporte**: si el reporte se mete como
  bottomsheet en home o como tab nueva, cambia el shape de
  navegación. Decisión a tomar en Fase 4.
- **Schema migration**: cualquier cambio destructivo a categorías o
  cuentas pre-alpha requiere `fallbackToDestructiveMigration` o un
  versionado serio. Fase 4 debería declarar política.
- **Tamaño del APK con kotlinx-serialization**: ~150 KB extra. Sigue
  bajo el límite blando de 15 MB que pide Fase 2. OK.

---

## 7. Inputs concretos para Fase 4 (Roadmap)

Estos son los insumos que Fase 4 va a empaquetar en sprints + milestones:

1. **9 Quick wins** (sección 4) → todos en un "Sprint 0" pre-roadmap, 1 día.
2. **5 Big rocks** (sección 5) → orden propuesto:
   1. BR-1 US-11 (Reporte) — primero, porque define si la app vale.
   2. BR-4 US-09 (Meses) — segundo, porque acopla con BR-1.
   3. BR-3 US-01/02 (Onboarding) — tercero, antes de testers.
   4. BR-2 US-18/19 (Export/Import) — cuarto, refuerza la promesa.
   5. BR-5 US-21 (Profile completo) — último, polish.
3. **Decisiones de diseño pendientes** (a tomar en Fase 4):
   - Reporte: donut vs barras vs combinación.
   - Export: kotlinx-serialization vs org.json.
   - Onboarding: 1 pantalla o flujo de 2-3 pantallas.
   - Navegación: tab nueva o botón desde Home para el reporte.
4. **Política a escribir antes de alpha**:
   - Qué tipos de feature requests se rechazan automáticamente
     (Fase 1 §6 riesgo #7).
   - Esquema de versionado JSON para futuras migraciones de
     import/export.

---

## 8. Lo que NO está en este doc (y por qué)

- **Performance benchmarks**: no medí FPS, GPU overdraw, ni tiempos
  reales de arranque. Fase 4 debería incluir una pasada de Layout
  Inspector + frame stats antes de alpha.
- **Auditoría de accesibilidad**: no se cubrió. Fase 2 §6 pide
  WCAG AA. Validar antes de alpha.
- **Auditoría de seguridad**: no aplica a v1 (sin red, sin secrets, sin
  almacenamiento sensible).
- **Cobertura de tests actual**: el agent reportó 17 archivos de test,
  pero no medí cobertura real. Recomiendo agregar al backlog un
  comando `./gradlew jacocoTestReport` o equivalente.

---

## 9. Chequeo final — preguntas para vos

Antes de aprobar este doc y pasar a Fase 4, leelo y respondé:

1. **Priorización de big rocks**: el orden que propongo
   (Reporte → Meses → Onboarding → Export → Profile) ¿te hace sentido,
   o querés un orden distinto? (Ejemplo: meter Onboarding antes para
   poder mostrar la app a alguien sin vergüenza.)
2. **Quick wins**: ¿alguno te incomoda? Especialmente QW-6 (currency
   ARS → PEN) que es destructivo en dev. ¿Listo para aceptar ese reset?
3. **Tiempo realista**: estimo ~4 semanas de calendario a 5-6 h/semana.
   ¿Ese ritmo es real para tu vida, o tenemos que recalibrar (más
   semanas, menos features, o más horas)?
4. **El gap más doloroso**: ¿hay algo de la sección 3 que no esperabas?
   Si algún gap te sorprende negativamente ("¿en serio no existe?"),
   decímelo — puede señalar un malentendido entre Fase 2 (lo que
   pedimos) y lo que asumías estaba hecho.

Si las 4 son "sí, así está bien" → firmamos y pasamos a Fase 4
(ROADMAP_V1.md con sprints concretos, Definition of Done por feature,
gate antes de Play Store alpha).
Si alguna es "no" → ajustamos antes de seguir.
