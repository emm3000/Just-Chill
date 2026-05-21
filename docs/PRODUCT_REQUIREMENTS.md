# PRODUCT REQUIREMENTS — JustChill v1

**Fase**: 2 de 5 (Requirements)
**Bloquea**: ARCHITECTURE_REVIEW.md (Fase 3) y ROADMAP_V1.md (Fase 4).
**Depende de**: PRODUCT_DISCOVERY.md (Fase 1, firmada).

> Cada requirement de este doc está justificado por una decisión de Fase 1.
> Si querés meter algo que no se justifica en Fase 1, NO se mete acá: se
> vuelve a Fase 1 y se renegocia el positioning primero. **Sin excepción**.

---

## 0. Resumen ejecutivo

JustChill v1 es una app Android **100% local**, **manual**, **gratis sin ads
ni paywall**, en **español**, en **soles**, para el **peruano 25-35 con
sueldo + ingresos extras**, cuyo pilar diferencial es **claridad de
ingresos múltiples + simplicidad radical (30 segundos al día)**.

**El alcance de v1 tiene 7 épicas** (E1-E7), **15 stories Must**, **6 Should**,
**5 Could** y **una lista explícita de 12 Won't have**. La métrica de éxito
de v1 es **retención semana 4** (% de usuarios que siguen registrando 5+
días por semana al mes de instalación).

**Criterio de aceptación global**: si una feature no le ahorra tiempo o no
aporta claridad a Sebastián, NO entra. No se mete por "queda bonito" ni por
"todas las apps lo tienen".

---

## 1. Cómo se priorizó

Framework: **MoSCoW** (Must / Should / Could / Won't).

- **Must (M)** — sin esto, no hay v1. La app no cumple su promesa.
- **Should (S)** — debería entrar en v1, pero si hay que cortar, se corta.
  Va a la primera ventana post-launch (1-2 semanas).
- **Could (C)** — agradable de tener, no decisivo. v2 o backlog.
- **Won't (W)** — explícitamente fuera de scope. Rechazado por decisión
  de positioning, no por falta de tiempo.

Cada story incluye un campo `Trazabilidad` con la(s) decisión(es) de Fase 1
que la justifica. Si no hay trazabilidad clara → la story no debería
existir.

---

## 2. La persona, en una frase

**Sebastián, 29, Lima.** Sueldo ~S/4,500 + side hustle S/800-3,000 variable.
Usa Yape, Plin, BCP, BBVA. Quiere saber **cuánto realmente entra cada mes
desde todas sus fuentes**, sin tener que ser una persona ordenada para
lograrlo. Abre la app **una vez al día, de noche**, antes de dormir.

Cada user story de este doc se valida con la pregunta: *"¿Sebastián, parado
en la cola del banco con una mano, en 15 segundos, puede hacer esto?"*. Si
la respuesta es no, la story está mal escrita o mal priorizada.

---

## 3. Épicas

| ID | Épica | Por qué existe |
|---|---|---|
| E1 | Onboarding sin fricción | Sin onboarding rápido, Sebastián abandona en los primeros 60 segundos. |
| E2 | Captura rápida de movimientos | El core del producto. Si tarda más de 15s, fracasa. |
| E3 | Panorama del día / del mes (home) | El "vistazo" — la respuesta de 1 segundo a "¿qué pasó hoy?". |
| E4 | Reporte de ingresos por fuente | La apuesta diferencial. Es el ajá moment. |
| E5 | Categorías y cuentas mínimas | Estructura sin ser estructura. Lo mínimo para que E2 y E4 funcionen. |
| E6 | Peruanidad concreta | Yape, Plin, BCP, BBVA como ciudadanos de primera clase. |
| E7 | Export / Import JSON manual | La salida — para que el usuario nunca se sienta atrapado. |

---

## 4. User stories

> Formato: `[ID] [Prioridad] Como <quién>, quiero <qué>, para <por qué>.`
> Cada story tiene **Criterios de aceptación** (bullets concretos,
> verificables) y **Trazabilidad** (qué decisión de Fase 1 la sostiene).

---

### E1 — Onboarding sin fricción

#### US-01 [M] Primer launch sin login
**Como** Sebastián que acaba de instalar JustChill,
**quiero** abrir la app y poder usarla inmediatamente,
**para** no sentir que es "otra app más que me pide cuenta".

**Criterios de aceptación**:
- Al abrir por primera vez, NO se pide email, teléfono, ni cuenta de
  Google.
- El primer launch a home tarda **<3 segundos** desde tap del ícono.
- No hay tutorial obligatorio. El usuario puede registrar su primer
  movimiento en la pantalla siguiente al primer launch.

**Trazabilidad**: Fase 1 §3 ("sin login"), §3.5 (manifiesto), §7 (decisiones
bloqueadas: cero auth).

---

#### US-02 [M] Manifiesto visible al primer launch
**Como** Sebastián curioso por una app nueva,
**quiero** leer en 5 segundos qué cree esta app,
**para** decidir si me representa o la borro ya.

**Criterios de aceptación**:
- Al primer launch (UNA sola vez), se muestra el manifiesto de Fase 1 §3.5
  en una pantalla limpia, scroll mínimo.
- Botón único: "Empezar". Sin "skip" porque no hay tutorial que saltar.
- NO se muestra de nuevo nunca, salvo desde Ajustes > Acerca de.

**Trazabilidad**: Fase 1 §3.5 (apuesta cultural — la marca habla por sí
misma).

---

#### US-03 [S] Cuentas y categorías por defecto (Perú)
**Como** Sebastián que no quiere configurar nada,
**quiero** que la app ya tenga cuentas y categorías peruanas listas,
**para** registrar mi primer Yape en 10 segundos.

**Criterios de aceptación**:
- Cuentas preset en español: **Efectivo, Yape, Plin, BCP, BBVA, Interbank,
  Scotiabank**.
- Categorías de **gasto** preset: Comida, Transporte, Mercado, Salud,
  Entretenimiento, Servicios, Otros.
- Categorías de **ingreso** preset: **Sueldo, Freelance, Ventas, Propinas,
  Otros** (5 categorías de ingreso de primera clase — esto es la apuesta
  diferencial de E4).
- Todas son editables y eliminables, pero existen desde el día 0.

**Trazabilidad**: Fase 1 §5 (peruanidad), §9.6 (top hueco #3: peruanidad
concreta).

---

### E2 — Captura rápida de movimientos

#### US-04 [M] Registrar un gasto en ≤3 taps, ≤15 segundos
**Como** Sebastián parado en la bodega después de pagar,
**quiero** abrir la app y registrar el gasto con una mano,
**para** que el acto de registrar no me dé pereza.

**Criterios de aceptación**:
- Desde home, botón "+" visible (FAB). Tap → pantalla de captura.
- Pantalla de captura tiene foco automático en el campo de monto.
- Categoría se elige con chips horizontales (no dropdown). Las 5 más
  usadas aparecen primero.
- Cuenta se elige con chips horizontales. La última usada está
  preseleccionada.
- Fecha por defecto: hoy. La cambia solo si la toca.
- Botón "Guardar" siempre visible, no se mueve con el teclado.
- **Tiempo objetivo de extremo a extremo**: <15s para usuario habitual.
  <30s para nuevo.

**Trazabilidad**: Fase 1 §3 (sin fricción, 30 seg/día), §5.5 (ritual
nocturno), §9.9 (Monefy es el benchmark de velocidad de registro).

---

#### US-05 [M] Registrar un ingreso es tan rápido como un gasto
**Como** Sebastián que acaba de recibir un Yape de un cliente,
**quiero** registrar el ingreso con el mismo flujo que un gasto,
**para** no sentir que "ingresos" es una feature secundaria.

**Criterios de aceptación**:
- Toggle gasto / ingreso es el primer elemento visible al abrir la
  captura. Visualmente claro (color, posición).
- El flujo de un ingreso es 100% paralelo al de un gasto. Misma cantidad
  de taps, misma posición de elementos.
- Categorías de ingreso (Sueldo / Freelance / Ventas / Propinas / Otros)
  aparecen con la misma jerarquía visual que las de gasto.

**Trazabilidad**: Fase 1 §5 ("UI da igual peso a ingresos que a gastos"),
§9.6 (top hueco #1: ingresos como ciudadano de primera clase).

---

#### US-06 [M] Editar y borrar un movimiento
**Como** Sebastián que registró mal un movimiento,
**quiero** corregirlo o borrarlo,
**para** no quedar con data sucia.

**Criterios de aceptación**:
- Tap largo o tap simple en un movimiento del listado → editar / borrar.
- Borrar pide confirmación una vez (no toast pasivo — confirmación
  explícita).
- Edición permite cambiar todos los campos del movimiento.

**Trazabilidad**: requisito funcional básico — sin trazabilidad especial.

---

#### US-07 [S] Búsqueda y filtros de movimientos
**Como** Sebastián que quiere saber cuándo le pagó cierto cliente,
**quiero** buscar por texto o filtrar por categoría,
**para** encontrar sin scroll infinito.

**Criterios de aceptación**:
- Pantalla "Ver" tiene barra de búsqueda + chips de categoría.
- Búsqueda con debounce de 300ms, sin botón "buscar".
- Filtros combinables (texto + categorías).
- Banner visible cuando hay filtros activos + botón para limpiar todo.

**Trazabilidad**: ya implementada en commit `0978273`. Se incluye acá
para completitud del scope v1.

---

### E3 — Panorama del día / del mes (home)

#### US-08 [M] Home muestra el mes en curso en 1 vistazo
**Como** Sebastián abriendo la app de noche,
**quiero** ver el panorama del mes actual sin tocar nada,
**para** que el "qué pasó" sea instantáneo.

**Criterios de aceptación**:
- Home muestra, sin scroll inicial:
  - **Total ingresos** del mes (grande, destacado).
  - **Total gastos** del mes (mismo peso visual).
  - **Saldo del mes** (ingreso − gasto).
  - Movimientos del día actual (lista corta).
- El número grande de ingresos es el **elemento visual dominante** de la
  home (consecuencia directa de la apuesta de Fase 1).
- Tiempo desde tap del ícono hasta home pintada: **<1 segundo** en
  Android de gama media.

**Trazabilidad**: Fase 1 §5 ("reporte estrella: ingresos por fuente"),
§5.5 (vistazo nocturno).

---

#### US-09 [M] Cambiar entre meses
**Como** Sebastián que quiere comparar con el mes pasado,
**quiero** navegar al mes anterior o siguiente,
**para** ver tendencia sin abrir otra pantalla.

**Criterios de aceptación**:
- Flechas o swipe horizontal en la home cambia el mes.
- Indicador de mes actual visible en todo momento.
- Saltar al "mes actual" en un solo tap (si estoy navegando atrás).

**Trazabilidad**: Fase 1 §5 (ritual nocturno + observación de tendencia).

---

#### US-10 [S] Saldo acumulado total (opcional)
**Como** Sebastián que quiere ver el balance histórico,
**quiero** ver el saldo total acumulado en algún lado,
**para** tener una idea del estado general.

**Criterios de aceptación**:
- En home o ajustes, pequeño indicador de saldo total acumulado.
- NO es el elemento principal — el principal es el mes en curso.
- Puede desactivarse desde ajustes (algunos usuarios prefieren no verlo).

**Trazabilidad**: requerimiento secundario. Se puede cortar.

---

### E4 — Reporte de ingresos por fuente (LA APUESTA)

#### US-11 [M] Reporte mensual de ingresos por fuente
**Como** Sebastián que terminó su primer mes con la app,
**quiero** ver un reporte que me diga cuánto entró por cada fuente,
**para** entender si mi side hustle está valiendo la pena.

**Criterios de aceptación**:
- Pantalla dedicada accesible desde home o tab.
- Visualización clara de **ingresos por categoría** del mes seleccionado.
  Ejemplo: *"Sueldo S/4,500 (60%) — Freelance S/2,000 (27%) — Ventas
  S/800 (11%) — Otros S/200 (2%)"*.
- Visualización: gráfico simple (donut o barras), no torta complicada.
- Comparación contra mes anterior, en porcentaje.
- Texto legible en screenshot (alto contraste, sin gráficos minúsculos),
  porque Sebastián los va a screenshotear para mostrar.

**Trazabilidad**: Fase 1 §3 (pilar diferencial), §5.5 (ajá moment), §9.6
(top hueco #1).

**Nota crítica**: este es **el** reporte que justifica la app. Si funciona,
la app vale la pena. Si no, no.

---

#### US-12 [S] Reporte mensual de gastos por categoría
**Como** Sebastián que ya entendió sus ingresos,
**quiero** ver también el desglose de gastos,
**para** saber dónde se me va la plata.

**Criterios de aceptación**:
- Mismo formato que US-11 pero para gastos.
- Accesible desde la misma pantalla con un toggle.
- Si v1 corta scope, este es el primer corte (Sebastián abre la app
  por ingresos, no por gastos — la mayoría de apps ya hace bien gastos).

**Trazabilidad**: feature derivada — no diferencia, pero la gente la espera.

---

#### US-13 [C] Comparativa de últimos 3-6 meses
**Como** Sebastián que ya usó 6 meses la app,
**quiero** ver tendencia de los últimos meses,
**para** decisiones más serias.

**Criterios de aceptación**:
- Gráfico de línea o barras: ingresos totales por mes, últimos 3-6 meses.
- Filtro opcional por fuente.

**Trazabilidad**: feature de retención. NO necesaria para v1, va a v2.

---

### E5 — Categorías y cuentas

#### US-14 [M] Crear, editar, borrar categorías
**Como** Sebastián que quiere ajustar las categorías por defecto,
**quiero** poder editarlas o crear las mías,
**para** que reflejen mi realidad.

**Criterios de aceptación**:
- Pantalla de gestión de categorías accesible desde ajustes.
- Diferenciar **categorías de ingreso** y **categorías de gasto**.
- Borrar una categoría con movimientos asociados muestra advertencia
  (los movimientos pasan a "Sin categoría", no se borran).

**Trazabilidad**: requisito funcional. Ya existe parcialmente en código.

---

#### US-15 [M] Crear, editar, borrar cuentas
**Como** Sebastián que quiere registrar una cuenta no preset,
**quiero** crear cuentas adicionales,
**para** no estar limitado a las 7 predefinidas.

**Criterios de aceptación**:
- Pantalla de gestión de cuentas accesible desde ajustes.
- Borrar una cuenta con movimientos asociados muestra advertencia.

**Trazabilidad**: requisito funcional. Ya existe parcialmente.

---

### E6 — Peruanidad concreta

#### US-16 [M] Símbolo S/ en todos los montos
**Como** Sebastián peruano,
**quiero** ver los montos con `S/`,
**para** que se sienta mi app y no una traducción.

**Criterios de aceptación**:
- Todos los montos visibles usan formato **`S/ X,XXX.XX`** con separador
  de miles `,` y decimales `.`.
- Sin opción de cambiar moneda en v1.

**Trazabilidad**: Fase 1 §7 (moneda v1: solo soles), §9.6 (top hueco #3).

---

#### US-17 [S] Bancos peruanos como cuentas preset
**Como** Sebastián con cuentas en bancos locales,
**quiero** que aparezcan BCP, BBVA, Interbank, Scotiabank ya listos,
**para** no tener que escribir cada uno.

**Criterios de aceptación**:
- Cubierto por US-03 (cuentas por defecto).
- Considerar íconos/colores característicos de cada banco (azul BBVA,
  azul BCP, etc.) **opcional, no bloqueante** — atención a posibles
  problemas de marca registrada.

**Trazabilidad**: Fase 1 §5 (Yape, Plin, bancos peruanos como ciudadanos
de primera clase).

---

### E7 — Export / Import JSON manual

#### US-18 [M] Exportar toda mi data a JSON
**Como** Sebastián que cambia de celular,
**quiero** sacar TODA mi data en un archivo JSON,
**para** no perder nada y para sentir que la app es realmente mía.

**Criterios de aceptación**:
- Ajustes > Exportar mi data → genera un archivo `.json` en el storage
  del usuario o vía share intent.
- Archivo contiene: todos los movimientos, todas las categorías, todas
  las cuentas, metadata (versión del schema, fecha de export).
- Archivo legible por humano (JSON indentado, no minificado).
- Sin compresión, sin encriptación adicional (es del usuario, hace lo
  que quiera con él).

**Trazabilidad**: Fase 1 §4 (ES) y §7 (decisión bloqueada: Export/Import
es v1), §3.5 (manifiesto: "solo vos, tu plata").

---

#### US-19 [M] Importar un JSON exportado
**Como** Sebastián que instaló JustChill en su celular nuevo,
**quiero** importar mi backup,
**para** continuar donde quedé.

**Criterios de aceptación**:
- Ajustes > Importar data → file picker → muestra preview de cuántos
  movimientos / cuentas / categorías van a entrar.
- Confirmación explícita antes de aplicar.
- Estrategia de conflictos: **reemplazar todo** (v1 simple) o **merge**
  (v2). En v1 elegimos **reemplazar todo** con confirmación brutal:
  *"Esto va a borrar lo que tengas hoy y poner lo del archivo."*
- Si el JSON está corrupto o no es de JustChill, error claro.

**Trazabilidad**: Fase 1 §4 (ES) y §7.

---

#### US-20 [C] Exportar / importar CSV (para Excel)
**Como** Sebastián que quiere hacer su propio análisis en Excel,
**quiero** exportar a CSV,
**para** abrirlo en Sheets.

**Criterios de aceptación**:
- Botón secundario en ajustes (no protagonista). JSON sigue siendo el
  principal.

**Trazabilidad**: feature de poder para power-users. v2 o backlog.

---

### Ajustes y meta

#### US-21 [M] Pantalla de ajustes mínima
**Como** Sebastián,
**quiero** una pantalla de ajustes sin sorpresas,
**para** encontrar lo que necesito sin perderme.

**Criterios de aceptación**:
- Sin más de **8 ítems** en ajustes (la fricción de "demasiadas opciones"
  es exactamente lo que la app rechaza).
- Ítems v1:
  1. Categorías
  2. Cuentas
  3. Exportar mi data
  4. Importar data
  5. Acerca de (incluye manifiesto)
  6. Política de privacidad (1 página, idioma humano)
  7. Versión de la app
- NO hay: tema oscuro (v2), idioma (v1 solo español), notif (v1 no hay
  notif).

**Trazabilidad**: Fase 1 §3.5 (anti-complejidad).

---

### Reporte mensual — vistas avanzadas

#### US-22 [S] Tendencias de 6 meses (Reporte → tab Tendencias)
**Como** Sebastián,
**quiero** ver mi tasa de ahorro y el flujo de los últimos 6 meses,
**para** confirmar de un vistazo si estoy mejorando o no, sin abrir Excel ni hacer cuentas.

**Criterios de aceptación**:
- En `ReportScreen`, al tope hay un segmented control `Mes / Tendencias`.
  Mes es la vista de US-11 actual. Tendencias es esta nueva US.
- Tendencias muestra tres bloques en este orden:
  1. **Tasa de ahorro · 6 meses**: porcentaje grande
     (`(ingresos - gastos) / ingresos` agregado sobre los últimos 6
     meses incluyendo el actual). Pill con delta vs los 6 meses
     previos (puntos porcentuales: `↑ 4 pts` o `↓ 3 pts`) si hay
     ≥12 meses de data; oculta si no. Frase de contexto debajo:
     *"De cada S/ 100 que entró, ahorraste S/ X. Mejoraste (o
     empeoraste o mantuviste) vs. los 6 meses previos."*
  2. **Entró vs Salió**: bar chart vertical 6 meses (§7.13 DS).
     Mes actual destacado en label. Promedio mensual (ingresos ·
     gastos) debajo.
  3. **Tus mayores gastos**: top 3 categorías de gasto de los
     últimos 6 meses, con monto total (no promedio) y meta
     *"Top en X de 6 meses"* (cantidad de meses en que esa categoría
     estuvo en el top 3 mensual).
- **Early state** (<3 meses de data en la app): no se renderiza el
  contenido; empty state con copy *"Vuelve cuando tengas más
  historial — Tendencias necesita al menos 3 meses para tener algo
  útil que mostrar."* — sin CTA (no hay acción que el usuario pueda
  tomar, solo esperar).
- Tendencias **no** tiene navegación de mes (es siempre "últimos 6
  meses hasta hoy"). La nav de mes vuelve cuando se selecciona Mes.
- Compartir reporte está disponible en ambos tabs vía botón en el
  app bar: `Intent.ACTION_SEND` con texto plano (`text/plain`) que
  resume el contenido visible del tab actual.

**Trazabilidad**: Fase 1 §2.3 (claridad sobre ingresos múltiples
extendida al "estás mejorando o no" del manifesto). **Caveat
honesto**: este tab roza la línea del manifesto (*"tu plata no
necesita un dashboard"*). Justificación: 1 pantalla, 3 bloques,
sin filtros ni configuración. Si en dogfooding aparece que la gente
no la abre, se diferia a v2 sin culpa.

---

## 5. Won't have (explícitamente fuera de scope)

Estas no se construyen, ni en v1 ni en v2. Cualquiera que pida una de
estas, la respuesta es "no, y leé el manifiesto". Si una se mueve a v2,
**hay que volver a Fase 1 y renegociar el positioning**.

| ID | No se hace | Razón (de Fase 1) |
|---|---|---|
| W-01 | **Conexión al banco / bank-sync** | §4 NO ES; §9.7 (Wallet caso de estudio de cómo arruina reviews). |
| W-02 | **Cloud backend propio** | §4 NO ES; §7 decisión bloqueada. |
| W-03 | **Google Sign-In / cuenta** | §3 (sin login). |
| W-04 | **Multi-cuenta compartida (parejas, equipos)** | §4 NO ES (es individual). |
| W-05 | **Presupuestos rígidos ("no gastes más de X")** | §4 NO ES (es observación, no policía). |
| W-06 | **Multi-moneda** | §4 NO ES; §7 (solo soles). |
| W-07 | **Cursos / lecciones de educación financiera** | §4 NO ES. |
| W-08 | **Notificaciones push diarias / gamificación / badges** | §3.5 (anti-complejidad). |
| W-09 | **Suscripción premium / paywall** | §7 (gratis sin paywall v1); §9.7 (paywall sobre features básicas es traición). |
| W-10 | **Anuncios** | §7 (sin ads). |
| W-11 | **Sync entre dispositivos** | §4 NO ES; §7 (backend NO entra). |
| W-12 | **OCR / lectura automática de notificaciones Yape** | Apuesta descartada en Fase 1 §3.5 (elegimos mensaje radical, no integración audaz). |

---

## 6. Criterios no funcionales (NFRs)

| Área | Requisito |
|---|---|
| **Tiempo de arranque** | <3s primer launch (cold), <1s siguientes (warm) en gama media (Android 8+, 3GB RAM). |
| **Tiempo de registro de movimiento** | <15s extremo a extremo para usuario habitual. |
| **Tamaño del APK** | Objetivo <15MB. Hard limit 25MB. |
| **Mínimo Android** | API 28 (Android 9). Acordado por proyecto actual. |
| **Idioma** | Solo español. Sin fallback a inglés, sin selector. |
| **Accesibilidad** | Texto escalable (sin breakage), contraste WCAG AA mínimo, content descriptions en elementos interactivos. NO se compromete a screen reader completo en v1. |
| **Offline** | 100% funcional sin red. NUNCA hace request HTTP en v1. La permission `INTERNET` se quita del manifest (es contradictoria con la promesa). |
| **Persistencia** | SQLDelight local. Crash o force-close no pierde data (transacciones atómicas). |
| **Batería** | Sin trabajo en background, sin WorkManager periódico, sin servicios. La app solo trabaja cuando está abierta. |
| **Crashes** | Crashlytics solo en builds `prodRelease`. Builds `dev` sin telemetría. |

---

## 7. Métricas de éxito de v1

### Métrica norte
**Retención semana 4**: % de usuarios que registran al menos 5 días/semana
durante la semana 4 desde instalación.
- **Objetivo v1**: 25% (benchmark de apps de finanzas LatAm es ~10-15%).
- Si está por debajo de 15%, hay un problema de producto, no de marketing.

### Métricas secundarias
- **Tiempo medio de registro de movimiento**: <15s.
- **% de usuarios que llegan al primer reporte mensual** (US-11): >40% de
  los instalados.
- **% que exportan al menos una vez** (US-18): >5% (señal de que confían
  lo suficiente como para querer mover su data).
- **Crashes-free sessions**: >99.5%.

### Métricas que NO usamos
- DAU / MAU (vanity, no dicen nada de un producto de ritual diario).
- Tiempo en app (queremos que sea CORTO).
- Notificaciones abiertas (no mandamos notificaciones).

---

## 8. Definition of Done para v1

Una feature está "lista para v1" cuando:

1. ✅ Implementada (todos los criterios de aceptación cumplidos).
2. ✅ Tiene tests de dominio si involucra lógica de negocio (use case + repositorio).
3. ✅ Funciona en Android API 28 y API 36 (probado manualmente).
4. ✅ Cumple los NFRs aplicables (especialmente tiempo de registro <15s
   para E2, tiempo de arranque para E3).
5. ✅ Copy en español de Perú (no neutro, no español de España).
6. ✅ Sin crash bajo input edge case (cantidades enormes, descripción
   vacía, fecha en el futuro, etc.).
7. ✅ Validación visual: hay screenshots, se ve bien en zoom (Fase 1 §5:
   "tiene que verse bonita en zoom").

JustChill v1 está lista para alpha en Play Store cuando:

- Todas las **15 Must** están en estado "lista".
- Al menos **3 de las 6 Should** están en estado "lista".
- Las **12 Won't** siguen sin construirse (no se filtraron features durante
  el desarrollo).
- 4 semanas mínimas de dogfooding propio + 5 testers reales antes del
  primer push a Play Store alpha.

---

## 9. Mapa rápido stories → épicas → Fase 1

| Story | Épica | Decisión Fase 1 que la sostiene |
|---|---|---|
| US-01 | E1 | §3 sin login, §7 cero auth |
| US-02 | E1 | §3.5 manifiesto |
| US-03 | E1 | §5, §9.6 peruanidad |
| US-04 | E2 | §3 sin fricción, §9.9 Monefy benchmark |
| US-05 | E2 | §5 ingresos = gastos en peso, §9.6 |
| US-06 | E2 | funcional básico |
| US-07 | E2 | ya implementado (`0978273`) |
| US-08 | E3 | §5 reporte estrella, §5.5 vistazo nocturno |
| US-09 | E3 | §5.5 |
| US-10 | E3 | secundario |
| US-11 | E4 | §3 pilar diferencial, §5.5 ajá, §9.6 |
| US-12 | E4 | derivado |
| US-13 | E4 | v2 retención |
| US-14 | E5 | funcional básico |
| US-15 | E5 | funcional básico |
| US-16 | E6 | §7 soles, §9.6 |
| US-17 | E6 | §5 bancos primera clase |
| US-18 | E7 | §4 ES, §7 export v1 |
| US-19 | E7 | §4 ES, §7 |
| US-20 | E7 | v2 |
| US-21 | meta | §3.5 anti-complejidad |

---

## 10. Resumen ejecutivo de prioridades

| Prioridad | Cantidad | IDs |
|---|---|---|
| Must (M) | 15 | US-01, 02, 04, 05, 06, 08, 09, 11, 14, 15, 16, 18, 19, 21 + US-17 si llega |
| Should (S) | 6 | US-03, 07, 10, 12, 17, otros marcados |
| Could (C) | 5 | US-13, 20 + tema oscuro v2 + comparativa multi-mes + íconos bancarios |
| Won't (W) | 12 | W-01 a W-12 (sección 5) |

---

## 11. Qué desbloquea este doc

Una vez firmado, **Fase 3 (Architecture Review)** puede arrancar. Fase 3
contrasta cada Must y Should de este doc contra el código existente y
produce el delta: *qué ya está hecho, qué falta, qué hay que rehacer
porque no encaja con v1, qué legacy hay que matar*.

---

## 12. Chequeo final — preguntas para vos

Antes de aprobar este doc y pasar a Fase 3, leelo y respondé:

1. ¿La lista de 15 Must se siente **realista** para un side project con
   5-15 h/semana, o es demasiado y tenés que cortar?
2. ¿Algún Won't te duele leerlo? (Esa es la pregunta más importante —
   si te duele, hay que renegociar Fase 1, no parcharlo acá.)
3. ¿Los criterios de aceptación están al nivel de detalle que necesitás
   para que un dev (vos o un sub-agent) los implemente sin preguntar 5
   veces, o están demasiado vagos / demasiado prescriptivos?
4. ¿La métrica norte (retención semana 4 ≥ 25%) te parece **ambiciosa
   pero alcanzable**, o estoy poniendo un número del aire?

Si las 4 son "sí, así está bien" → firmamos y pasamos a Fase 3.
Si alguna es "no", ajustamos antes de seguir.
