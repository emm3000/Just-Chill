# ROADMAP — JustChill v1 alpha

**Fase**: 4 de 5 (Roadmap a Play Store alpha)
**Bloquea**: Fase 5 (Post-v1 + growth).
**Depende de**: PRODUCT_DISCOVERY.md (Fase 1), PRODUCT_REQUIREMENTS.md (Fase 2),
ARCHITECTURE_REVIEW.md (Fase 3) — las 3 firmadas.

> Este doc es el plan **calendarizado** para llegar a Play Store alpha.
> Sprints de 1 semana, definition of done por feature, gate explícito
> antes de publicar, y plan B si algo se rompe. Si el plan se sale de
> tiempo, **NO se agregan features ni se reduce calidad** — se mueve
> la fecha. Esa es la política.

---

## 0. Resumen ejecutivo

**Meta v1**: publicar **alpha cerrada en Play Store** con 5-10 testers
reales (Sebastiánes de verdad), 15 Must + 3-6 Should funcionando, métricas
instrumentadas, y un gate de calidad cumplido.

**Tiempo realista a alpha**: **8-10 semanas calendario** a 5-6 horas
productivas por semana. ~55-70 horas totales de trabajo de código + polish
+ dogfooding.

**Tiempo de Fase 3 (4 semanas) fue optimista** — asumía ritmo de 12-15
h/semana. Este roadmap va con 5-6 h/sem que es lo que vos firmaste en
Fase 1.

**Hito target**: si arrancamos hoy (2026-05-17), alpha en Play Store
**~2026-07-19 (lunes)** + 1 semana de buffer = **2026-07-26**. Esa es la
fecha que tu calendario debería tener marcada.

**Cómo cortamos si nos atrasamos**: cortamos Should antes que calidad.
Cortamos US-12 (reporte gastos), US-10 (saldo acumulado), US-17 (íconos
bancarios). Nunca cortamos: US-11, US-18/19, US-01/02. Esos son la app.

---

## 1. Capacidad asumida (la honestidad de Fase 1)

| Variable | Valor | Origen |
|---|---|---|
| Horas/semana | 5-6 productivas (no calendario) | Fase 1 §1 |
| Velocidad de código en este repo | ~10-12 LOC/h efectivas con tests | estimación honesta side project |
| Esfuerzo total v1 | ~55-70 h | Fase 3 §5 + 20% overhead realista |
| Buffer riesgos | 20% (incluye debugging, scope creep micro, vida) | estándar planificación |
| Sprint duration | 1 semana calendario | Estándar |

**Si vos podés hacer 10+ h/semana, este roadmap se comprime a ~5 semanas.**
No lo asumo porque Fase 1 dijo 5-15 con sesgo a 5.

---

## 2. Mapa de sprints

| Sprint | Semanas | Foco | Output |
|---|---|---|---|
| **S0** | Sem 1 (parcial) | Cleanup (9 quick wins) | Repo limpio, base sana |
| **S1** | Sem 1-3 | BR-1 Reporte de ingresos (la apuesta) | US-11 + US-09 funcionando |
| **S2** | Sem 4 | BR-3 Onboarding + manifiesto | US-01, US-02 funcionando |
| **S3** | Sem 5-6 | BR-2 Export/Import JSON | US-18, US-19 funcionando |
| **S4** | Sem 7 | BR-5 Profile completo + Should opcionales | US-21 + lo que entre |
| **S5** | Sem 8 | Polish + accesibilidad + screenshots | App lista visualmente para Play Store |
| **S6** | Sem 9 | Dogfooding propio intensivo | Bugs detectados y fixeados |
| **S7** | Sem 10 | Testers externos + gate de alpha | 5-10 testers + publicación |

**Total**: 10 semanas. Si vamos bien, S6-S7 se acortan y publicamos antes.

---

## 3. Sprint 0 — Cleanup (semana 1 parcial)

**Objetivo**: arrancar limpio. Sin estos quick wins, el código carga peso
muerto y el roadmap empieza con deuda.

**Tiempo estimado**: ~2 horas. Cabe en parte de la primera semana, dejando
3-4h para arrancar S1.

### Tareas

| # | Tarea | Tiempo | Riesgo |
|---|---|---|---|
| QW-1 | Quitar `INTERNET` permission del `AndroidManifest.xml` | 1 min | 0 |
| QW-2 | Quitar dep `androidx.constraintlayout` | 5 min | 0 |
| QW-3 | Quitar deps `navigation.fragment.ktx`, `navigation.ui.ktx`, `lifecycle.livedata.ktx` | 5 min | bajo (verificar build pasa) |
| QW-4 | Borrar botón "Cerrar sesión" de `ProfileScreen` | 5 min | 0 |
| QW-5 | Actualizar `CLAUDE.md` (sacar mención de tablas legacy que no existen) | 5 min | 0 |
| QW-6 | Cambiar default currency `ARS → PEN` o eliminar campo | 20-30 min | medio (reset destructivo dev) |
| QW-7 | Agregar 2 categorías ingreso al seed (Ventas, Otros) | 5 min | 0 |
| QW-8 | Wiring Categorías + Cuentas en `ProfileScreen` (links) | 20 min | 0 |
| QW-9 | Mostrar versión app en `ProfileScreen` desde `BuildConfig` | 10 min | 0 |

### Definition of Done — S0
- [ ] `./gradlew assembleDevDebug` pasa.
- [ ] `./gradlew test` pasa (todos los tests siguen verdes).
- [ ] APK pesa menos que antes (medir antes/después: `./gradlew :app:bundleDevDebug` y mirar tamaño).
- [ ] App arranca y funciona como antes.
- [ ] 1 commit por quick win (granularidad para rollback si algo se rompe).

### Gate para pasar a S1
Si QW-6 (currency) genera problemas, **se posterga**, no se bloquea S1.
La reasignación de moneda puede vivir en una migración separada.

---

## 4. Sprint 1 — Reporte de ingresos (la apuesta) — semanas 1-3

**Objetivo**: que exista la **única feature que justifica la app**.
Sin este sprint, no hay v1. **Es el sprint más largo y más crítico.**

**Tiempo estimado**: 16-24 horas. ~3 semanas a 6 h/sem.

### Decisiones de diseño a tomar AL ARRANCAR S1 (antes de tirar código)

1. **Donut vs barras vs ambos**: recomendación → **barras horizontales**.
   Razones: legibilidad en zoom (Fase 1 §5), más fácil de comparar 5
   categorías, mejor screenshot. Donut es bonito pero distorsiona los
   porcentajes pequeños.
2. **Tab nueva vs botón desde Home**: recomendación → **botón "Ver
   reporte" debajo del balance hero en Home**. Razones: descubrible para
   Sebastián que abre por hábito, no obliga una tab que se ve vacía
   las primeras 3 semanas.
3. **Mes selector**: ya viene de BR-4 (US-09), se integra al reporte.
4. **Comparativa mes anterior**: en línea (texto + delta), no gráfico
   adicional. Ej: *"+12% vs Abril"*.

### Plan de implementación

**Semana 1 — Domain + Data (~5-6h)**
- US-09 acoplado: agregar `currentMonth: YearMonth` a `HomeUiState`,
  `PreviousMonth` / `NextMonth` / `JumpToToday` intents, y refactor de
  `HomeViewModel` para recombinar Flow con mes seleccionado.
  - Tests: cambio de mes, salto a hoy, edge cases (enero → diciembre
    año anterior).
- Nueva query `monthlyIncomeByCategory(start, end)` en `transactions.sq`.
- `GetMonthlyIncomeByCategoryUseCase` en `:domain`.
- `GetMonthlyComparisonUseCase` para delta vs mes anterior.

**Semana 2 — Use cases + tests (~5-6h)**
- Completar use cases del reporte.
- Tests de dominio (MockK + `runTest`): %, totales, mes vacío, una sola
  categoría, comparación negativa.
- DI en módulo Koin.

**Semana 3 — UI + integración (~6-8h)**
- `ReportScreen.kt`, `ReportViewModel`, `ReportUiState`,
  `ReportIntent`, `ReportEffect` (MVI completo).
- Componente `IncomeByCategoryBars` (barras horizontales con %, monto,
  nombre categoría).
- Indicador de comparativa con mes anterior.
- Navegación desde Home → Report.
- Estado vacío diseñado (texto: *"Aún no registraste ingresos este mes
  — anotá el primero y volvé al final del mes"*).
- Chevrons de navegación entre meses dentro del reporte.

### Criterios de aceptación — S1
- [ ] US-09: navegación entre meses funciona en Home Y en Reporte.
- [ ] US-11: reporte muestra ingresos por categoría con %, monto y
      comparativa mes anterior.
- [ ] Las 5 categorías de ingreso del seed (Sueldo, Freelance, Ventas,
      Propinas, Otros) aparecen como ciudadanas de primera clase.
- [ ] Texto legible en zoom 200% (test manual con Layout Inspector).
- [ ] Performance: pantalla pinta en <500ms incluso con 1000+
      transacciones (test con data sintética).
- [ ] Tests de dominio cubren el use case completo.

### Riesgos S1
- **Riesgo**: el reporte se siente vacío en mes con pocos movimientos.
  Mitigación: estado vacío diseñado + nudge a registrar.
- **Riesgo**: gráfico no se ve "screenshoteable". Mitigación:
  validación visual con 3 screenshots reales antes de cerrar el sprint.
- **Riesgo**: SQL lento con muchas categorías. Mitigación: índice en
  `(type, date, categoryId)` si no existe (validar en S0 o S1 semana 1).

---

## 5. Sprint 2 — Onboarding + manifiesto — semana 4

**Objetivo**: que el primer launch hable de la marca. Sin esto, instalar
JustChill se siente igual que instalar Monefy.

**Tiempo estimado**: 6-8 horas.

### Plan

- Introducir `DataStore Preferences` (~30 min: agregar dep, crear
  wrapper en `:app/core/preferences/AppPreferences.kt`).
- `FirstLaunchScreen.kt`: Compose, fondo limpio, manifiesto de Fase 1
  §3.5 renderizado con tipografía generosa y mucho espacio (Fase 1 §5
  "tiene que verse bien en zoom"). Botón único: "Empezar".
- `FirstLaunchViewModel`: marcar flag `first_launch_seen = true` cuando
  el usuario tap Empezar.
- En `MainActivity`: leer flag DataStore antes de decidir navegación
  inicial. Mientras lee, splash neutro (1 frame, no flicker).
- Acceso desde ajustes: en `ProfileScreen`, ítem "Acerca de" abre la
  misma pantalla en modo "ya visto" (sin guardar flag de nuevo).

### Criterios de aceptación — S2
- [ ] Al instalar y abrir por primera vez, se ve el manifiesto.
- [ ] Botón "Empezar" lleva a Home.
- [ ] Al cerrar y reabrir la app, va directo a Home (manifiesto NO se
      muestra de nuevo).
- [ ] Desde ProfileScreen → Acerca de, se puede leer el manifiesto otra
      vez.
- [ ] Tiempo cold start total: <3 segundos (medir con `am start -W`).
- [ ] Test de UI básico (Compose test): renderiza correctamente.

### Riesgos S2
- **Riesgo**: DataStore async genera flicker. Mitigación: usar
  `runBlocking` solo en `Application.onCreate` para leer flag, o un
  splash mínimo (50ms) en `MainActivity`.

---

## 6. Sprint 3 — Export/Import JSON — semanas 5-6

**Objetivo**: que el usuario nunca se sienta atrapado en JustChill.
Si pierde el celular, si quiere cambiar de app, si quiere analizar en
Excel — su data sale en un archivo.

**Tiempo estimado**: 12-16 horas. 2 semanas a 6 h/sem.

### Decisiones de arranque S3

1. **Librería**: `kotlinx-serialization` (typesafe, ~150 KB, ya está
   pensado para esto).
2. **Schema JSON**: versionado con campo `schemaVersion: 1`. Cuando
   evoluciones, migración explícita.
3. **Estrategia import**: **reemplazar todo** (Fase 2 US-19). Sin merge
   en v1. Confirmación brutal: *"Esto va a borrar lo que tengas hoy y
   poner lo del archivo."*

### Plan

**Semana 5 — Export (~6h)**
- Agregar dep `kotlinx-serialization` + plugin Kotlin serialization en
  `:data` y `:domain`.
- DTOs serializables en `:domain/shared/io/`:
  `ExportPayloadDto`, `TransactionDto`, `CategoryDto`, `AccountDto`,
  con campo `schemaVersion`.
- `ExportDataUseCase`: trae todo de los repos, construye payload,
  retorna `String` JSON indentado.
- `:data/shared/io/` wrappers de file write (`OutputStream` desde
  `ActivityResultContract.CreateDocument`).
- `ProfileViewModel`: estado MVI con `Exporting`, `ExportSuccess`,
  `ExportError`. Intent `ExportData`.
- UI: botón Exportar → file picker (save document) → guarda JSON →
  snackbar "Listo".
- Tests: roundtrip básico (serialize → deserialize → comparar).

**Semana 6 — Import + polish (~6-8h)**
- `ImportDataUseCase`: valida `schemaVersion`, valida estructura,
  reemplaza data (transaction-wrapped: borra todo + inserta del JSON).
  Si falla a la mitad, rollback.
- File read wrapper en `:data/shared/io/`.
- UI: botón Importar → file picker → diálogo de confirmación brutal →
  reemplaza → snackbar "Listo, X movimientos importados".
- Tests: import válido, import corrupto, import con versión futura,
  import vacío.

### Criterios de aceptación — S3
- [ ] US-18: archivo JSON exportado contiene todos los movimientos,
      categorías, cuentas, metadata, `schemaVersion`.
- [ ] JSON es legible por humano (indentado, sin caracteres raros).
- [ ] US-19: import muestra preview de cuántos items, pide confirmación,
      reemplaza todo o falla limpio.
- [ ] Import de JSON corrupto NO crashea — error visible.
- [ ] Roundtrip completo (export → import en celular nuevo) recupera
      el estado idéntico.
- [ ] Tests cubren los casos felices y los errores.

### Riesgos S3
- **Riesgo**: file picker en API 28 vs API 36 se comporta distinto.
  Mitigación: testar en ambos emuladores.
- **Riesgo**: import a la mitad deja la DB inconsistente. Mitigación:
  envolver TODO el reemplazo en una transacción SQLDelight; si falla,
  rollback automático.
- **Riesgo**: el JSON crece mucho con miles de movimientos. Mitigación:
  v1 no lo soluciona (es un side project, no Spotify). Documentar
  límite informal: ~10k movimientos sin problema.

---

## 7. Sprint 4 — Profile completo + Should opcionales — semana 7

**Objetivo**: cerrar `ProfileScreen` y meter las Should que entren
sin atrasar el plan.

**Tiempo estimado**: 6 horas.

### Plan

- `ProfileScreen` final (lista de 7 ítems de Fase 2 US-21):
  1. Categorías → ya wireado en S0 ✅
  2. Cuentas → ya wireado en S0 ✅
  3. Exportar mi data → ya hecho en S3 ✅
  4. Importar data → ya hecho en S3 ✅
  5. Acerca de → reabre `FirstLaunchScreen` (manifiesto)
  6. Política de privacidad → pantalla simple con texto plano, 1 página
     de español humano: *"Tu data vive en tu celular. No la mandamos a
     ningún servidor. Si exportas un archivo, vos decidís qué hacés con
     él. No usamos cookies, no usamos analytics de tu uso, no tenemos
     servidor."*
  7. Versión de la app → ya hecho en S0 ✅
- Si sobra tiempo (probable):
  - US-12: reporte de gastos por categoría (toggle en la pantalla del
    reporte, comparte 80% del código de US-11).
  - US-17: íconos genéricos por banco en `AccountsScreen` (sin marcas
    registradas — solo colores).

### Criterios de aceptación — S4
- [ ] ProfileScreen tiene los 7 ítems funcionando, ninguno es dummy.
- [ ] Política de privacidad redactada en español humano, no legalese,
      ≤ 250 palabras.
- [ ] Si entraron US-12 o US-17, tienen sus criterios cumplidos.

---

## 8. Sprint 5 — Polish + accesibilidad + screenshots — semana 8

**Objetivo**: la app pasa de "funciona" a "está lista para mostrarse".

**Tiempo estimado**: 6 horas.

### Plan

- **Audit visual** (~2h):
  - Layout Inspector en cada pantalla. Buscar overdraw, jank, fonts
    chicos, padding inconsistente.
  - 3 capturas de cada pantalla principal (Home, Reporte,
    AddTransaction, ProfileScreen) en celular gama media — si una se ve
    mal, fix.
- **Accesibilidad mínima** (Fase 2 §6) (~2h):
  - `contentDescription` en todos los íconos interactivos (FAB,
    flechas de mes, botones de export/import).
  - Verificar contraste WCAG AA con plugin (ej. Accessibility Scanner).
  - Test manual con TalkBack en las 3 pantallas clave (no garantizamos
    soporte completo, solo "no es vergonzante").
- **Performance** (~1h):
  - `am start -W` cold start en 3 emuladores distintos.
  - Frame stats en cada pantalla principal.
  - Si alguna pantalla cae por debajo de 60 FPS, fix.
- **Polish copy** (~1h):
  - Todos los strings de error, vacío, confirmación: lenguaje humano,
    español peruano coloquial (no neutro).
  - Sin "Por favor", sin "Ha ocurrido un error", sin "Operación
    exitosa". Más como: *"No pude exportar — capaz no hay espacio en tu
    celu?"*

### Criterios de aceptación — S5
- [ ] Sin overdraw evidente en pantallas principales.
- [ ] Contraste WCAG AA en texto.
- [ ] Cold start <3s, warm start <1s.
- [ ] Capturas listas para Play Store (formato 1080×1920, sin
      placeholders).
- [ ] Todos los textos visibles auditados por estilo.

---

## 9. Sprint 6 — Dogfooding propio intensivo — semana 9

**Objetivo**: vos mismo usá la app **5+ días esta semana**, registrando
TODO. Tomá notas.

**Tiempo estimado**: uso ambient (no horas dedicadas) + ~3-4h para
fixes detectados.

### Lo que vas a hacer
- Instalar build `prodRelease` en tu celular principal.
- Borrar/exportar tu data anterior, empezar limpio.
- Registrar cada movimiento real durante 5-7 días.
- Anotar (en un `.md` aparte, no en la app):
  - Cada vez que dudás "¿esto cómo se hace?"
  - Cada vez que sentís fricción en el flujo
  - Cada vez que algo no se ve bien
  - Cada vez que algo te sorprendió positivamente

### Output
- Lista de **bugs** (cosas rotas).
- Lista de **frictions** (cosas que funcionan pero molestan).
- Lista de **nice surprises** (cosas que te enamoraron — guardalas
  para el copy de Play Store).
- Fix de los bugs críticos. Las frictions van a backlog Fase 5.

### Gate para pasar a S7
Si en S6 encontrás un bug que rompe el flujo principal (no se puede
registrar, no se puede ver el reporte, export crashea) → **postergar
alpha 1 semana** y fixear. No es negociable. La app no sale rota.

---

## 10. Sprint 7 — Testers externos + publicación alpha — semana 10

**Objetivo**: 5-10 personas reales usando la app **por 2 semanas**
mientras está en alpha cerrada en Play Store.

**Tiempo estimado**: 4-6 horas (setup) + monitoreo durante 2 semanas.

### Pre-publicación checklist

- [ ] App ID configurado en Play Console (cuenta dev USD 25 si no
      existe).
- [ ] Listado del Play Store con texto, capturas, ícono, política de
      privacidad URL (puede ser un Gist).
- [ ] Internal testing track creado.
- [ ] Lista de 5-10 emails de testers (te recomiendo: 2 amigos cercanos
      que sí van a usar, 2 conocidos del target Sebastián, 1-2 más
      "duros" que vayan a romper cosas).
- [ ] Build firmado release (no debug).

### Setup del feedback loop
- Canal de WhatsApp grupal con los testers (o 1-a-1 si preferís
  intimidad).
- Pregunta semanal: *"¿cuántos días la abriste esta semana?, ¿algún
  problema?"*.
- Anotás respuestas en un doc separado.

### Criterios de aceptación — S7
- [ ] App publicada en alpha cerrada.
- [ ] 5+ testers la instalaron.
- [ ] Al menos 3 la abrieron en los primeros 3 días.
- [ ] Sin crash reports en Crashlytics durante la primera semana.
- [ ] Feedback inicial registrado (para Fase 5).

---

## 11. Pre-alpha gate (cuándo está lista para publicar)

Antes de subir a Play Store, **TODAS estas cosas deben ser sí**:

### Funcionalidad
- [ ] Las 15 Must implementadas (US-01, 02, 04, 05, 06, 08, 09, 11, 14,
      15, 16, 18, 19, 21 + una opcional).
- [ ] Al menos 3 de las 6 Should implementadas.
- [ ] **Cero feature de las 12 Won't** (verificar manualmente — esta
      es la prueba de disciplina del proyecto).

### Calidad
- [ ] `./gradlew test` pasa al 100%.
- [ ] No hay crashes durante 5+ días de uso propio (Sprint 6).
- [ ] Cold start <3s, registro <15s, registro <3 taps.
- [ ] APK <25 MB (objetivo <15 MB).
- [ ] Sin `INTERNET` permission en manifest.

### Marca
- [ ] Manifiesto se muestra al primer launch.
- [ ] Tono del copy es "irreverente sin insultar" — no hay strings
      corporativos, no hay "Por favor".
- [ ] El símbolo `S/` aparece en todos los montos.
- [ ] Cuentas preset peruanas listas desde día 0.

### Distribución
- [ ] Listado del Play Store redactado (título, descripción corta,
      descripción larga, capturas, ícono).
- [ ] Política de privacidad pública (URL accesible).
- [ ] 5-10 testers identificados, contactados, listos.

**Si UNO solo de estos checks está rojo → no se publica. Se posterga
1 semana, se cierra el gap, se reevalua.**

---

## 12. Métricas que vamos a trackear durante alpha

Las que ya están firmadas en Fase 2 §7, instrumentadas en código antes
de S7:

| Métrica | Cómo medirla | Objetivo v1 |
|---|---|---|
| Retención semana 4 (5+ días/sem) | Manual en S7 (preguntar a testers) + Crashlytics events si los instrumentamos | ≥25% |
| Tiempo medio de registro | Test manual con cronómetro + telemetría opcional | <15s |
| % usuarios que llegan a primer reporte mensual | Pregunta directa a testers tras 30 días | >40% |
| % que exportan al menos una vez | Pregunta directa a testers | >5% |
| Crash-free sessions | Crashlytics dashboard | >99.5% |

**Lo que NO trackeamos**: DAU/MAU, tiempo en app, notifs (no mandamos).

**Decisión pendiente para implementar**: ¿agregamos un evento de
Firebase Analytics tipo "first_report_viewed"? Sería útil pero
contradice el espíritu local-only. **Recomendación: no en v1.** Se
mide preguntando a los 5-10 testers, que son pocos. Si en v2 escalamos,
revisamos.

---

## 13. Plan B (qué cortamos si nos atrasamos)

Si en cualquier sprint perdemos >50% del scope previsto, se aplica
esta priorización de cortes (en este orden):

1. **Cortar US-12** (reporte de gastos) si no entra en S4. La app
   funciona sin él.
2. **Cortar US-17** (íconos bancarios) — siempre cortable.
3. **Cortar US-10** (saldo acumulado) — opcional desde día 0.
4. **Cortar US-20** (export CSV) — ya estaba en Could.
5. **Postergar S5 (polish) parcialmente** — solo lo crítico de
   accesibilidad y performance, dejar el resto para post-alpha.
6. **Postergar testers externos** — si llegamos justo, hacemos S7 con
   3 testers en vez de 10.

**Lo que NO se corta nunca, bajo ninguna circunstancia**:
- US-11 (reporte ingresos por fuente) — es la app.
- US-18/US-19 (export/import) — es la promesa.
- US-01/US-02 (onboarding + manifiesto) — sin esto es Monefy.
- Cualquier ítem del pre-alpha gate de calidad.

---

## 14. Post-alpha (qué pasa después de publicar)

Esto va más profundo en Fase 5, pero el resumen:

- **Semanas 11-12**: alpha cerrada en Play Store, testers usando.
- **Semana 13**: review honesta con vos mismo. Métricas reales. Decidir:
  - ¿Pasamos a beta abierta?
  - ¿Cortamos features de v2 según feedback?
  - ¿Cambiamos algo del positioning? (si pasa esto, volvemos a Fase 1.)
- **Semana 14+**: estrategia de outreach a creators peruanos
  (3-5 chicos primero, ver Fase 5).

---

## 15. Riesgos del roadmap (cosas que pueden romperlo)

| Riesgo | Probabilidad | Mitigación |
|---|---|---|
| Vida personal te come 2-3 semanas | Alta | Buffer del 20% ya incluido. Si pasa, mover la fecha sin pánico. |
| Bloqueo técnico en Reporte (S1) | Media | Diseño + decisiones tomadas antes de tirar código. Si Donut vs Barras genera duda, pegarle al sub-agent para prototipos rápidos. |
| Export/Import más complejo de lo previsto | Media | Es la sección con más superficie nueva. Tener Buffer de 1 semana en S3. |
| Feedback de Sprint 6 dice "esto no es lo que pensé" | Baja-media | Si pasa, volvemos a Fase 2 y reevaluamos antes de S7. NO publicamos algo que no nos gusta. |
| Play Console rechaza por algo del listado | Baja | Validar manualmente con sus guidelines antes de S7. |
| Crashlytics en `prodRelease` se rompe sin INTERNET | Baja | Verificar en S5 que el flavor prod sigue compilando y funcionando offline. |

---

## 16. Chequeo final — preguntas para vos

Antes de aprobar este doc y pasar a Fase 5 (post-v1 + growth), respondé:

1. **Fecha target**: 2026-07-26 (10 semanas). ¿Eso te cierra
   personalmente? Si tenés un evento, vacaciones, mudanza,
   compromiso que pisa esas semanas, recalibremos AHORA.
2. **Capacidad real**: 5-6 h/semana es lo que asumimos. ¿Es honesto, o
   estás sobreestimando? Mejor planear con menos y entregar antes.
3. **Sprint 0 listo para arrancar?**: los 9 quick wins se pueden hacer
   esta semana (~2h). Si decís sí, te puedo arrancar S0 ahora mismo.
4. **Plan B**: la lista de cortes (sección 13). ¿Aceptás que si nos
   atrasamos, esos son los primeros que caen? Si alguno te duele,
   movámoslo en la lista antes de empezar.

Si las 4 son "sí" → firmamos Fase 4 y arrancamos S0 cuando vos digas.
Si alguna es "no" → ajustamos.

**Una vez firmado este doc, hay una decisión adicional**: ¿pasamos a
Fase 5 (Post-v1 + growth) ahora, o paramos acá y arrancamos S0 con
los planes existentes? Mi recomendación: **firmar Fase 5 antes de
empezar a codear**, porque la estrategia de outreach a creators y los
hooks de monetización futura pueden cambiar pequeñas decisiones de v1
(ej: dejar lugar en el schema para un campo "premium tier" futuro,
aunque no se use).
