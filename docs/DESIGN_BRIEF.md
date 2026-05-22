# JustChill — Design Brief

> Documento para la diseñadora. Self-contained: no necesitas leer otros
> archivos del repo para hacer tu trabajo. Si algo está confuso,
> pregúntame antes de empezar a diseñar.

---

## 0 · TL;DR

JustChill es una app **Android** de finanzas personales para **peruanos
25-35 con sueldo + ingresos extras**. **100% local** (sin login, sin
cloud, sin sync), en **español**, **soles**. La versión actual ya
funciona y va a alpha cerrada en Play Store. **El diseño visual lo armé
yo como dev** — quiero que vos lo rediseñes desde cero o lo ajustes, vos
decidís cuánto tocar.

- **13 pantallas** en total (ver §5).
- **4 tabs** en bottom bar + botón central "Agregar".
- **Tema oscuro actualmente**, abierto a tema claro o ambos.
- **Sin Figma todavía** — vos lo armás. Yo te paso APK + screenshots + video.

---

## 1 · Contexto producto

### 1.1 Quién es el usuario

**Persona**: Sebastián, 29, vive en Lima (Surco/Miraflores/Jesús María).

- Trabaja en una empresa mediana (marketing, tech, comercial). Sueldo
  ~S/4,500.
- Tiene un **side hustle**: consultoría freelance, vende algo por
  Instagram, o contenido. Le entra entre S/800 y S/3,000 más al mes,
  **muy variable**.
- Usa Yape y Plin diariamente. Tiene cuenta BCP (sueldo) y BBVA
  (ahorros). A veces le pagan en efectivo.
- Probó Fintonic, Monefy, Wallet — abandonó porque eran complicadas,
  pedían login, o no entendían el contexto peruano.
- Sabe cuánto es su sueldo. **No sabe cuánto realmente gana al mes** si
  suma todo.

### 1.2 Por qué existe la app

> "Mira cuánto realmente entra y sale, en 30 segundos al día."

El usuario quiere ver con claridad sus **ingresos múltiples**
(sueldo + cachuelos + Yape de clientes) y entender en qué se le va la
plata, **sin que sea un proyecto**. Apps competidoras tratan el
"income" como una categoría más; JustChill lo trata como protagonista.

### 1.3 Manifiesto (el tono de la marca)

Esta es la primera pantalla que ve el usuario al instalar. Es la postura,
no la descripción del producto:

```
Tu plata no necesita
un dashboard.

Necesita tu atención,
30 segundos al día.

Sin login.
Sin notificaciones.
Sin que te vendamos nada.

Solo tú, tu plata,
y la verdad.
```

**Sabor de marca**: provocador-minimalista. Frases cortas, mucho aire.
Tono "tú" (NO "vos" — esto NO es Argentina). Irreverente sin insultar.
**El enemigo declarado es la complejidad innecesaria.**

### 1.4 Momento de uso

- **Ritual nocturno**: 1×/día, 5-7 días por semana, antes de dormir.
- Por sesión: 2-5 movimientos del día (Yapes, consumo, transferencia).
- Tiempo objetivo: <30 segundos en la app por sesión.
- **Aha moment**: el primer reporte de fin de mes (día ~30) — *"este
  mes entraron S/6,200, 60% sueldo, 30% Yapes, 10% otros"*.

### 1.5 Referencias visuales que usé (no son obligatorias, podés
descartarlas)

- **Starlink mobile app** — feel arquitectónico, monocromo, disciplinado.
- **Apple Wallet** — el monto como héroe tipográfico, sin chrome.
- **Linear** — densidad medida, accentos restringidos.
- **Things 3** — ritmo entre items de lista, padding generoso.

**NO son referencias**: Mint, YNAB, Monefy, Wallet (todas las apps de
finanzas tradicionales). Esas se ven cargadas y coloridas — es lo que
NO queremos.

---

## 2 · Lo que NO se renegocia (constraints duros)

Estos puntos están firmados desde el proceso de discovery. Si tu diseño
los rompe, va a chocar con producto.

| Constraint | Por qué |
|---|---|
| **Español peruano** (tú, no vos) | Target Lima |
| **Soles (S/)** como única moneda visible | Perú-first |
| **100% local**: cero login, cero "iniciar sesión", cero "tu cuenta" | Postura de privacy |
| **Sin notificaciones push** (excepto un único recordatorio opcional de reporte mensual, opt-in) | Postura "no molestar" |
| **Sin gamificación, sin badges, sin streaks** | Postura "no es Duolingo" |
| **Yape, Plin, BCP, BBVA, Interbank, Scotiabank, Efectivo** son ciudadanos de primera clase, no settings | Peruanidad concreta |
| **No usamos colores de marcas registradas** (no morado Yape, no azul BCP) | Riesgo legal |
| **Manual** — el usuario tipea cada movimiento. No conectamos al banco | Es feature, no bug |

---

## 3 · Lo que SÍ está abierto a rediseño

**Todo lo demás**. Específicamente:

- Paleta de colores (actualmente monocromo dark — podés proponer light,
  podés proponer otra paleta entera).
- Tipografía (actualmente Inter — abierto a otra).
- Layouts de cada pantalla.
- Sistema de íconos (actualmente Material Symbols Outlined).
- Componentes y su jerarquía visual.
- Microinteracciones y animaciones.
- Empty states, error states, copy de UI (te paso lo actual como base,
  podés reescribir).
- Bottom navigation vs. otra estructura de navegación (te paso la actual
  pero podés cuestionarla).

**La regla**: si te parece que algo no aporta al ritual de 30 seg/día,
proponé sacarlo o repensarlo.

---

## 4 · Mapa de navegación

### 4.1 Estructura general

```
Manifiesto (solo 1ª vez o desde Perfil > Acerca de)
   │
   ▼
[Bottom bar persistente con 4 tabs + botón Agregar]
   │
   ├── Inicio (Home)            ──► Reporte (push)
   │                            ──► Editar movimiento (push)
   │
   ├── Ver (lista movimientos)  ──► Editar movimiento (push)
   │
   ├── [+] Agregar (modal-like) ──► Nuevo movimiento
   │                                  └── Seleccionar categoría
   │                                        └── Nueva categoría (in-flow)
   │
   ├── Cuentas                  ──► Nueva cuenta (push)
   │                            ──► Nueva categoría (push)
   │                            (Editar / borrar = modal dialog)
   │
   └── Perfil                   ──► Categorías (push)
                                ──► Cuentas (push, mismo destino que tab)
                                ──► Exportar (system picker)
                                ──► Importar (system picker → dialog)
                                ──► Acerca de (re-muestra Manifiesto)
                                ──► Privacidad (push)
```

### 4.2 Bottom bar (4 tabs + 1 botón)

| Posición | Label | Ícono actual | Propósito |
|---|---|---|---|
| 1 | Inicio | Home | Dashboard mensual |
| 2 | Ver | AttachMoney (S/) | Lista buscable de transacciones |
| 3 | **+ Agregar** | **+** (centro, color accent) | **Botón principal — crea movimiento** |
| 4 | Cuentas | PlaylistAddCheckCircle | Gestión de cuentas |
| 5 | Perfil | Person | Settings + backup + acerca de |

**Nota**: el botón "+ Agregar" está en el bottom bar como un item más,
no como FAB tradicional. **Esta decisión es cuestionable** — proponé
algo mejor si tenés una idea (FAB clásico, swipe-up, gesto, etc.). El
usuario tiene que poder registrar un movimiento en **≤3 taps desde que
abre la app** (criterio US-04).

### 4.3 Bottom bar visibility

El bottom bar se **oculta automáticamente** en pantallas modales/push
(formularios, picker de categoría, manifiesto). Reaparece en las 4 tabs
+ Reporte. Esto es una decisión que también podés cuestionar.

---

## 5 · Inventario de pantallas (13 total)

Para cada pantalla: propósito, cómo se entra/sale, jerarquía actual,
estados especiales, interacciones, y notas/problemas que ya identifiqué.

> Los nombres de componentes Compose entre paréntesis son referencias
> para que entiendas qué piezas reutilizo hoy. No te ates a ellos.

---

### 5.1 Manifiesto — `ManifestoScreen.kt`

- **Propósito**: presentar la propuesta de valor / postura al primer
  install. Re-accesible desde Perfil > Acerca de.
- **Entrada/salida**: 1ª vez es la pantalla inicial; revisita es push
  desde Perfil.
- **Jerarquía visual (top → bottom)**:
  - Column full-screen centrado verticalmente
  - 4 estrofas (display style, 36sp, spacing 40dp entre estrofas):
    1. "Tu plata no necesita un dashboard."
    2. "Necesita tu atención, 30 segundos al día."
    3. "Sin login. Sin notificaciones. Sin que te vendamos nada."
    4. "Solo tú, tu plata, y la verdad." *(en color secondary — voz
       bajada al final)*
  - Botón primary full-width: "Empezar" (primera vez) o "Volver"
    (revisita)
- **Estados especiales**: ninguno. Estática.
- **Interacciones**: tap botón → entra a la app o vuelve.
- **Notas**: la copy es el héroe. **No agregues logo, ilustración,
  ni animación de entrada** — si lo hacés, perdiste la postura. Sí
  podés repensar la tipografía y el ritmo entre estrofas.

---

### 5.2 Inicio (Home) — `HomeScreen.kt`

- **Propósito**: dashboard mensual. El usuario lo abre y en <1s
  responde *"¿cómo me fue este mes?"*.
- **Entrada/salida**: tab raíz. Empuja a Reporte y a Editar movimiento.
- **Jerarquía visual (top → bottom)**:
  - **Selector de mes** (componente `MonthSelector`): `← MAYO 2026 →`
    + botón "Hoy" visible solo si no es el mes actual.
  - **Hero de balance** (`BalanceHero`):
    - Label "BALANCE"
    - Monto grande (amount.hero, 48sp)
  - **Resumen del mes** (`MonthSummary`, Row de 2 columnas iguales):
    - "INGRESOS" + monto
    - "GASTOS" + monto
  - **Botón "Ver reporte completo"** (Row con borde, 48dp alto): ícono
    chart + label + chevron derecho.
  - **Header "RECIENTES"** (label + link "Ver todas" a la derecha).
  - **Lista de últimas 3-5 transacciones** (`EmmListItem`): ícono
    tinted + descripción + fecha/hora + monto.
- **Estados especiales**:
  - Empty state (sin transacciones): ícono + "Sin transacciones
    recientes".
  - Mes futuro vacío: balance e ingresos/gastos en cero, lista vacía.
- **Interacciones**:
  - Tap ← / → → cambia mes.
  - Tap "Hoy" → salta a mes actual.
  - Tap "Ver reporte completo" → push a Reporte.
  - Tap "Ver todas" → switch a tab "Ver".
  - Tap movimiento → push a Editar.
- **Notas / problemas**:
  - El `MonthSelector` queda "pegado" al top al scrollear — no es sticky
    real, podría diseñarse mejor.
  - El balance es neutro (sin color) — abierto a coloreo semántico
    (verde si positivo, rojo si negativo) pero atenti a la regla
    "color no carga jerarquía".
  - "Ver reporte completo" es la entrada al **aha moment** — debería
    sentirse atractivo, hoy es solo un botón secundario.

---

### 5.3 Ver (lista de movimientos) — `SeeTransactionsScreen.kt`

- **Propósito**: ver TODO el historial, con búsqueda y filtros por
  categoría.
- **Entrada/salida**: tab. Empuja a Editar movimiento al tocar un item.
- **Jerarquía visual (top → bottom)**:
  - Título "Transacciones" (headline)
  - **Input de búsqueda** (`SearchHeader`): leading ícono Search +
    placeholder "Buscar por descripción" + trailing Close si hay query.
  - **Chips de categoría horizontales** (`CategoryChipsRow`,
    LazyRow): filtros toggleables.
  - **Banner "Limpiar filtros"** (`ActiveFilterBanner`): aparece si
    hay query o chips activos.
  - **Lista agrupada por día** (`TransactionList`): header con la
    fecha + items (mismo `EmmListItem` que Home).
- **Estados especiales**:
  - Empty total (nunca registró nada): ícono Receipt + "Aún sin
    transacciones" + CTA opcional.
  - Empty por filtros (hay data pero el filtro no matchea): ícono
    SearchOff + "Sin resultados para «query»" + botón "Limpiar
    filtros".
- **Interacciones**: buscar, filtrar por chip, limpiar filtros, tap
  item → editar.
- **Notas / problemas**:
  - La búsqueda hoy es solo por descripción, no por monto. Discutible.
  - Los chips son `FilterChip` de Material — podés reemplazar por
    chips custom si querés un estilo más limpio.
  - Hoy no hay scroll-to-top, ni jump-to-month dentro de esta lista
    (si el usuario tiene 6 meses de data, scrollear es largo).

---

### 5.4 Cuentas — `AccountsScreen.kt`

- **Propósito**: gestionar las cuentas (Yape, BCP, Efectivo, etc.).
- **Entrada/salida**: tab. Empuja a Nueva cuenta / Nueva categoría.
- **Jerarquía visual**:
  - Top bar: título "Cuentas" + ícono ⋮ (MoreVert) → DropdownMenu
    "Nueva cuenta" + "Nueva categoría".
  - Lista de cuentas (`AccountRow`):
    - Ícono según tipo (Bank, Cash, CreditCard, Investment)
    - Nombre de la cuenta
    - Ícono ⋮ → DropdownMenu "Editar" + "Borrar"
    - Border inferior 1dp entre rows
- **Estados especiales**:
  - Empty state: "Aún sin cuentas" + CTA "Crear cuenta".
  - Modal "Editar cuenta" (AlertDialog): solo cambia nombre.
  - Modal "¿Borrar cuenta?" (AlertDialog): con warning sobre
    movimientos asociados.
- **Notas / problemas**:
  - **El ⋮ doble (uno en top bar, uno por row) es confuso** — el del
    top bar abre "crear", el de cada row abre "editar/borrar".
  - Las cuentas no tienen ícono custom (solo el genérico por tipo) — la
    diseñadora puede proponer un sistema visual para diferenciar Yape
    de Plin de BCP **sin usar colores/logos de marca**.
  - No hay reordenamiento drag-drop.

---

### 5.5 Nueva cuenta — `AddAccountScreen.kt`

- **Propósito**: formulario para crear cuenta.
- **Entrada/salida**: push desde Cuentas. Vuelve atrás al guardar.
- **Jerarquía visual**:
  - Top bar: Close + título "Nueva cuenta".
  - Texto explicativo corto.
  - Input "NOMBRE" (underline only).
  - Dropdown "TIPO DE CUENTA" (Banco / Efectivo / Tarjeta / Inversión).
  - Dropdown "MONEDA" (PEN / USD).
  - Botón "Crear cuenta" (disabled si nombre vacío).
- **Notas / problemas**:
  - **No hay presets de cuentas peruanas** todavía — el usuario tipea
    "Yape" como texto libre. Podrías diseñar una vista de "seleccionar
    desde presets" antes de pasar a manual.
  - Dropdowns custom (no Material full input) con bottom border y
    chevron animado.

---

### 5.6 Perfil — `ProfileScreen.kt`

- **Propósito**: hub de configuración + backup + info.
- **Entrada/salida**: tab. Empuja a Categorías, Cuentas, Privacidad.
  Dispara system pickers para Export/Import. Re-muestra el manifiesto
  como "Acerca de".
- **Jerarquía visual**:
  - Título "Perfil"
  - Sección "GESTIONAR" (label small + filas):
    - Categorías (ícono Category) → push
    - Cuentas (ícono AccountBalanceWallet) → push
  - Sección "RESPALDO":
    - Exportar (ícono FileDownload) → system file picker
    - Importar (ícono FileUpload) → system file picker → AlertDialog
      "¿Reemplazar tu data?" → confirma o cancela
  - Sección "APP":
    - Acerca de (ícono Info) → re-abre manifiesto
    - Privacidad (ícono Shield) → push
  - Footer: "Versión 2.0.0-alpha" (caption tertiary)
- **Notas / problemas**:
  - Cada `ProfileRow` tiene chevron a la derecha. Sentís el patrón
    como "settings de iOS antiguo" — la diseñadora puede repensarlo.
  - El AlertDialog de import muestra copy peruana: *"¿Reemplazar tu
    data? Esto va a borrar todo lo que tengas hoy y poner lo del
    archivo."* + botones "Reemplazar todo" (en danger) y "Cancelar".

---

### 5.7 Privacidad — `PrivacyPolicyScreen.kt`

- **Propósito**: explicar la postura de privacy en lenguaje humano.
- **Entrada/salida**: push desde Perfil. Botón "Volver".
- **Jerarquía visual**:
  - Título "Tu privacidad" (headline)
  - 5 párrafos cortos (bodyL):
    1. "Tu plata vive en tu celular."
    2. "No la mandamos a ningún servidor. No tenemos servidor."
    3. "Si exportas tu data a un archivo, tú decides qué hacer con él
       — guardarlo, mandarlo o borrarlo."
    4. "No usamos analytics. No usamos cookies. No tenemos manera de
       saber qué cuentas o categorías creaste."
    5. "Si reinstalas la app o cambias de celular sin exportar primero,
       la data se pierde. Es el precio de no tener servidor — y nos
       parece justo."
  - Botón "Volver" primary full-width.
- **Notas**: la copy ya está aprobada por mí. Podés repensar layout,
  ritmo, jerarquía tipográfica, pero el contenido es el mismo.

---

### 5.8 Reporte — `ReportScreen.kt` ⭐ *La pantalla estrella*

- **Propósito**: el **aha moment** del producto. Mostrar de dónde
  vienen los ingresos del mes (o adónde se fueron los gastos), con
  comparativa vs. mes anterior.
- **Entrada/salida**: push desde Home. Botón ← para volver.
- **Jerarquía visual**:
  - Top bar: ← + "Reporte"
  - `MonthSelector` (mismo de Home)
  - **Toggle Ingresos / Gastos** (`ToggleIncomeExpense`, 2 botones)
  - Total del mes (amount.L) + subtítulo "ingresos en el mes" o
    "gastos en el mes"
  - Comparativa: *"+12% vs Abril"* en success/danger
  - Divisor
  - **Barras horizontales por categoría** (`IncomeByCategoryBars`):
    una barra por cada categoría, ordenadas desc por monto, con
    label, monto, % del total
- **Estados especiales**:
  - Empty: "Aún no registraste ingresos/gastos este mes" + CTA
    "Anotar ingreso/gasto".
- **Notas / problemas**:
  - **Es la pantalla más importante visualmente** — la idea es que sea
    "screenshoteable" para que el usuario la mande por WhatsApp y los
    amigos pregunten "qué app es esa". Hoy las barras son funcionales
    pero podrían ser memorables.
  - Las barras no son interactivas (no se puede tap para drill-down).
    Podrías proponer que sí.
  - El toggle Ingresos/Gastos podría ser un swipe horizontal en lugar
    de botones — pensalo.

---

### 5.9 Nuevo movimiento (Add) — `AddTransactionScreen.kt` ⭐ *La pantalla más usada*

- **Propósito**: registrar un ingreso o gasto en <15 segundos.
- **Entrada/salida**: push desde el botón **+** del bottom bar. Cierra
  con ← (Close). Puede abrir el SelectCategoryScreen como push.
- **Jerarquía visual**:
  - Top bar: Close + título "Nuevo gasto" / "Nuevo ingreso" + link
    "Limpiar" (visible si hay cambios)
  - **Monto** (`AmountHeroInput`): número GRANDE en el centro
    (amount.hero 48sp). El cursor parpadea ahí desde que se monta la
    pantalla.
  - **Sign chip ± **: pegado al monto, toggle "+" (ingreso) vs "−"
    (gasto). Cambia el color, el título de la pantalla, y el set de
    categorías disponibles.
  - **Categoría** (`CategorySelectorSection`):
    - Label "CATEGORÍA"
    - LazyRow de chips (cada chip = ícono + nombre)
    - Chip final "Más" → push a Seleccionar categoría
  - **Fila de chips meta** (2 chips lado a lado):
    - "FECHA" → tap abre DatePickerDialog
    - "CUENTA" → tap abre BottomSheet con lista de cuentas
  - **Nota colapsable** (`NoteSection`):
    - Default: link "+ Agregar nota"
    - Expandido: TextInput "NOTA" con placeholder "Opcional"
  - **Botón "Anotar gasto"** (full-width, sticky bottom): label
    dinámico — si falta monto dice "Ingresa un monto", si falta cuenta
    dice "Selecciona una cuenta".
- **Estados especiales**:
  - Disabled hasta que el monto y la cuenta están válidos.
  - Reset visible solo si `hasChanges`.
- **Notas / problemas**:
  - **Esta pantalla la iteré 3 veces y sigue sin convencerme**.
    Específicamente:
    - El sign chip ± junto al monto es claro pero no se siente "fluido"
      — querría algo más natural para alternar ingreso/gasto.
    - Los chips de categoría como LazyRow son funcionales pero no
      escalan visualmente cuando hay 15+ categorías.
    - El "+ Agregar nota" colapsable es bueno pero el tap target es
      pequeño.
  - Es la pantalla a la que el usuario llega 5-7 veces por semana.
    Si esta pantalla se siente bien, el producto funciona. Si chirría,
    no.

---

### 5.10 Editar movimiento — `EditTransaction.kt`

- **Propósito**: editar un movimiento existente, o eliminarlo.
- **Entrada/salida**: push desde Home o Ver al tocar un item.
- **Jerarquía visual**: idéntica a Nuevo movimiento, pero con dos
  diferencias:
  - Top bar tiene ícono Delete (papelera, color danger) a la derecha
    en lugar de "Limpiar".
  - **No tiene el `CategorySelectorSection`** — la categoría no se puede
    cambiar (decisión de producto cuestionable).
  - Botón final dice "Guardar cambios" en lugar de "Anotar gasto".
- **Estados especiales**:
  - AlertDialog "¿Eliminar transacción?" al tap delete: copy *"Esta
    acción no se puede deshacer"* + botones "Eliminar" (danger) y
    "Cancelar".
- **Notas / problemas**:
  - La inconsistencia "AddTransaction tiene categoría editable, Edit
    no" es un bug de UX que la diseñadora puede flaggear o proponer
    cómo unificar.

---

### 5.11 Categorías — `CategoriesScreen.kt`

- **Propósito**: gestionar categorías (crear, editar nombre, borrar).
- **Entrada/salida**: push desde Perfil o desde el ⋮ de Cuentas.
- **Jerarquía visual**:
  - Top bar: ← + "Categorías" + botón **+** (push a Nueva categoría)
  - Sección "INGRESOS" + lista (`CategoryRow`)
  - Sección "GASTOS" + lista (`CategoryRow`)
  - Cada row: nombre + ⋮ (DropdownMenu "Editar" / "Borrar")
- **Estados especiales**:
  - Empty: "Aún sin categorías" + CTA.
  - Edit dialog: AlertDialog con input "NOMBRE".
  - Delete dialog: AlertDialog con warning *"Los movimientos asociados
    pasarán a «Sin categoría»."*
- **Notas / problemas**:
  - **Las categorías hoy no muestran su ícono ni su color** en esta
    pantalla — solo el nombre. Esto es raro porque en `AddCategoryScreen`
    sí se eligen ambos. Hay que mostrarlos acá.
  - El orden es alfabético — no es customizable.

---

### 5.12 Nueva categoría — `AddCategoryScreen.kt`

- **Propósito**: crear categoría con nombre, tipo, ícono y color.
- **Entrada/salida**: push desde Categorías, desde Cuentas, o desde
  Seleccionar categoría (in-flow durante Nuevo movimiento).
- **Jerarquía visual**:
  - Top bar: ← + "Nueva categoría"
  - **Preview chip en vivo** (`PreviewChip`): ícono + nombre con el
    estilo final. Se actualiza mientras el usuario escribe / elige.
  - Input "NOMBRE"
  - **TIPO** (Row con sign chip ± + texto "Ingreso" / "Gasto")
  - **ÍCONO** (LazyRow horizontal de íconos circulares, ~20 opciones)
  - **COLOR** (LazyRow horizontal de círculos de color, ~6-8 opciones)
  - Botón "Crear «nombre»" (full-width, dinámico: "Escribe un nombre"
    si vacío).
- **Notas / problemas**:
  - El preview en vivo está bueno — mantener o mejorar.
  - Los íconos disponibles vienen de `AppIconCatalog` (Material Symbols
    Outlined). Podés proponer otro set.
  - Los colores son los del design system actual (`cat.slate`, `cat.sage`,
    `cat.terracotta`, `cat.mauve`, `cat.ochre`, `cat.graphite`) — 6
    tonos mutados. Podés proponer otra paleta de categorías pero
    mantené la regla "saturación baja, ~6 tonos máx".

---

### 5.13 Seleccionar categoría — `SelectCategoryScreen.kt`

- **Propósito**: picker de categoría desde el flujo de Nuevo
  movimiento. Permite crear categoría on-the-fly si la que el usuario
  busca no existe.
- **Entrada/salida**: push desde el chip "Más" de Nuevo movimiento.
  Vuelve atrás al seleccionar. Puede pushear a Nueva categoría con el
  nombre prerrellenado.
- **Jerarquía visual**:
  - Top bar: ← + "Selecciona categoría"
  - Input de búsqueda (Search + Close trailing)
  - Toggle de tipo (sign chip ± + label "Ingresos" + count
    "N categoría(s)")
  - Lista de categorías (LazyColumn, ícono + nombre, divider 1dp).
  - Botón secondary "+ Nueva categoría" (sticky bottom).
- **Estados especiales**:
  - Empty con query: *"Sin resultados para «query»"* + botón
    "+ Crear «query»" (muy buen detalle UX, mantener).
  - Empty sin query: "No tienes categorías de ingreso/gasto".
- **Notas / problemas**:
  - No hay check visual de "categoría actualmente seleccionada".
  - El divider 1dp se siente denso si hay 20 categorías — la diseñadora
    puede repensar la densidad.

---

## 6 · Componentes reutilizables

Estos son los componentes que aparecen en múltiples pantallas. Si los
rediseñas, se actualizan en cascada.

| Componente | Aparece en | Propósito |
|---|---|---|
| `MonthSelector` | Home, Reporte | `← MAYO 2026 →` + botón "Hoy" |
| `EmmListItem` | Home, Ver | Fila de transacción: ícono + descr + fecha + monto |
| `SignChip` | Nuevo mov, Editar mov, Nueva cat, Seleccionar cat | Toggle "+" / "−" para Ingreso vs Gasto |
| `AmountHeroInput` | Nuevo mov, Editar mov | Input grande del monto (48sp) |
| `EmmTextInput` | Múltiples formularios | Input estándar (underline only) |
| `EmmButton` | Múltiples | Botones primary / secondary / ghost / destructive |
| `ToggleIncomeExpense` | Reporte | Toggle binario entre Ingresos / Gastos |
| `IncomeByCategoryBars` | Reporte | Barras horizontales por categoría |
| `CategoryChip` | Nuevo mov | Chip de categoría en LazyRow (ícono + nombre) |
| `MetaChip` | Nuevo mov, Editar mov | Chip "FECHA: …" / "CUENTA: …" |
| `AccountPickerBottomSheet` | Nuevo mov, Editar mov | BottomSheet con lista de cuentas |
| `ProfileRow` | Perfil | Fila de settings (ícono + label + chevron) |

---

## 7 · Flujos clave (user journeys)

Estos son los caminos críticos que el usuario va a recorrer. **Si el
diseño no los hace fluidos, el producto no funciona.**

### 7.1 Primer launch (onboarding)

1. Instala desde Play Store, abre.
2. Ve el **manifiesto** (5.1) → tap "Empezar".
3. Aterriza en **Home** (5.2). No hay nada, está vacío.
4. Idealmente quiere registrar su primer movimiento.

**Objetivo**: que en <30s desde abrir la app esté en el formulario de
Nuevo movimiento. **Lo que falta hoy**: no hay onboarding contextual
para guiarlo a registrar el primero. Empty state de Home tiene un CTA
pero no es prominente.

### 7.2 Anotar un gasto (el flujo más usado, 5-7×/sem)

1. Abre la app → Home.
2. Tap botón **+** del bottom bar.
3. **Nuevo movimiento** (5.9):
   - Tipea el monto (focus automático en el input).
   - El sign chip ya está en **−** porque por default es gasto (asumimos
     gasto, no ingreso, porque es lo más común — discutible).
   - Selecciona categoría (chip de Comida, por ejemplo).
   - La fecha por default es hoy.
   - Selecciona cuenta (Yape).
   - Tap "Anotar gasto".
4. Vuelve a Home, ve el movimiento en "Recientes".

**Objetivo**: ≤3 taps + tipeo del monto, en ≤15 segundos.
**Friction actual**: hay que abrir el BottomSheet de cuenta cada vez
(podría haber default "última cuenta usada"). El sign chip ± podría no
ser necesario si la pantalla se entra con "modo gasto" preconfigurado.

### 7.3 Anotar un ingreso (más raro pero más alto valor visual)

Igual que 7.2 pero el usuario tiene que **cambiar el sign chip a "+"**.
Cuando lo hace:
- El título cambia a "Nuevo ingreso".
- Los chips de categoría cambian al set de Ingresos (Sueldo, Freelance,
  Ventas, Propinas, Otros).
- El botón final dice "Anotar ingreso".

**Objetivo**: que cambiar de gasto a ingreso sea **un solo gesto** —
hoy es un tap al sign chip, pero podría ser un swipe.

### 7.4 Ver el reporte mensual (el aha moment, ~día 30)

1. Abre app → Home.
2. Ve el balance + resumen.
3. Tap "Ver reporte completo" (botón secundario hoy).
4. **Reporte** (5.8):
   - Por default muestra **Ingresos** (es la apuesta del producto).
   - Ve el total + comparativa + barras por categoría.
   - Tap toggle → ahora ve **Gastos**.
5. Cierra con ← (vuelve a Home).

**Objetivo**: que la pantalla sea **screenshoteable**, memorable,
sienta como un "moment" no como un report más.

### 7.5 Buscar un movimiento viejo

1. Abre app → tab "Ver" (5.3).
2. Escribe en el buscador o filtra por categoría.
3. Encuentra el movimiento, tap → **Editar** (5.10).
4. Modifica o elimina.

**Friction actual**: la búsqueda es solo por descripción. Si el usuario
busca "S/ 50" no lo encuentra. Si busca por mes específico, tampoco
(hay que scrollear).

### 7.6 Hacer backup (export)

1. Tab "Perfil".
2. Tap "Exportar".
3. Se abre el system file picker (es nativo de Android, no diseñable
   desde la app).
4. Elige carpeta y nombre, guarda.
5. Snackbar: *"Listo, tu data está guardada."*

**Objetivo**: que el usuario entienda **qué archivo se descargó** y
**adónde está**. Hoy es opaco — la diseñadora puede proponer un dialog
post-export que aclare ("Tu backup está en Descargas/justchill-backup-…").

### 7.7 Restaurar backup (import)

1. Tab "Perfil".
2. Tap "Importar".
3. Se abre el system file picker.
4. Elige el JSON.
5. **AlertDialog**: *"¿Reemplazar tu data? Esto va a borrar todo lo
   que tengas hoy y poner lo del archivo."* + botones "Reemplazar
   todo" (danger) y "Cancelar".
6. Snackbar con count de movimientos importados.

**Sentimiento crítico**: este es el momento más vulnerable del
producto (data destructiva). El AlertDialog tiene que sentir
"sabés lo que estás haciendo" sin asustar.

---

## 8 · Sistema visual actual (referencia, no obligatorio)

Lo que está implementado hoy, para que entiendas el punto de partida:

- **Tema**: dark único. Fondo `#000000`, texto principal `#FAFAFA`.
- **Tipografía**: Inter (variable, weights 400-700) con `tnum` para
  números.
- **Espaciado**: base 4dp (s1=4, s2=8, s4=16, s6=24, s8=32, s10=40,
  s12=48).
- **Radios**: por default 0 (sharp), `radius.s`=6 para inputs/chips,
  `radius.m`=12 para cards/sheets, `radius.full`=999 para chips
  circulares.
- **Elevación**: **no hay shadows** — la separación es por diferencia
  de surface color + hairlines de 1dp.
- **Color**: monocromo + 6 tonos de categoría desaturados + 4
  semánticos (success / warning / danger / info).
- **Íconos**: Material Symbols Outlined, 24dp default.

**Carpeta del repo donde vive todo esto**:
`app/src/main/kotlin/com/emm/justchill/core/theme/`

> **No te ates a este sistema.** Si tu propuesta es tema claro o
> mezcla, dale. Si proponés cambiar a SF Pro / Manrope / lo que sea,
> dale. Lo que sí: respetar contraste AA mínimo (4.5:1 texto normal,
> 3:1 texto grande), targets táctiles ≥48dp, soporte de font scaling
> del sistema.

---

## 9 · Qué necesito de vos

### 9.1 Entregable mínimo

- **Figma** (preferido) o equivalente, con:
  - Las 13 pantallas en su estado base.
  - Estados especiales que consideres críticos (empty, error, loading
    si aplica).
  - Componentes en una página separada (design system básico:
    tokens de color, tipografía, espaciado, botones, inputs, list
    items, chips).
- **2-3 flujos prototipados** (clicables, mínimo: primer launch +
  anotar gasto + ver reporte) — para que yo entienda transiciones.
- **Spec mínimo por componente**: padding, tamaño de tipo, color del
  token (no hex literal — referenciá los tokens del sistema).

### 9.2 Entregable bonus (si tenés tiempo)

- Microinteracciones / animaciones que consideres clave.
- Propuesta de copy si pensás que la mía no funciona (yo escribí toda
  la actual).
- Casos extremos: ¿cómo se ve Home con 200 movimientos? ¿Cómo se ve
  Reporte si el usuario sólo tiene 1 categoría?
- Propuesta de iconografía / ilustraciones si pensás que el producto
  necesita "warmth" más allá del minimalismo.

### 9.3 Cómo trabajar

- **APK debug**: te lo paso por WhatsApp / Drive para que lo
  instales en tu Android y toquetees el producto real.
- **Video walkthrough**: 3-5 min grabados con scrcpy mostrándote los
  flujos.
- **Screenshots**: carpeta `docs/design-brief/screenshots/` (la voy a
  armar al mandar este doc).
- **Iteración**: prefiero revisar 1-2 veces (no 5) — armás propuesta
  completa → te doy feedback consolidado → ajustás.

### 9.4 Lo que NO necesito

- Brand identity nueva (logo, naming). El nombre se queda. El logo
  actual es texto plano, podés proponerlo pero no es prioritario.
- Landing page / sitio web (en v1 no hay).
- Diseño de notificaciones push (no tenemos).
- Onboarding tutorial multistep (la postura es "no onboarding"). El
  manifiesto es el onboarding.
- Account / login screens (no existen).

---

## 10 · Preguntas que probablemente tengas

> Las que ya anticipo. Si tenés otras, escribime.

**¿Puedo proponer cambiar la arquitectura de navegación (no bottom
bar)?**
Sí, pero defendelo. El bottom bar funciona en Android y permite ≤3 taps
a registrar movimiento. Si proponés algo distinto (drawer, gesto,
swipes), explicame por qué es mejor *para este usuario*.

**¿Puedo proponer tema claro o un acento de color real (no monocromo)?**
Sí. La regla "monocromo" la puse yo, no producto. Si tu propuesta tiene
color, asegurate de que:
- No usa colores asociados a bancos peruanos (riesgo legal).
- El color tiene función semántica clara, no decorativa.

**¿Por qué la copy es informal ("tú", no "usted")?**
Es el tono de marca firmado en discovery. Mantenelo. Si proponés
ajustes de copy, sigan en "tú".

**¿Por qué la app se ve "vacía" (mucho espacio negro)?**
Es decisión de producto. La regla es "el monto es el héroe, todo lo
demás recede". Si proponés un layout más denso, mostrame que sigue
respetando esa regla.

**¿Puedo proponer features nuevas?**
No para esta versión. v1 está scopeada y arrancando alpha. Si tenés
ideas de features, anotalas aparte y las vemos para v2.

---

## 11 · Lista de pantallas (versión corta para checklist)

| # | Pantalla | Archivo | Tipo | Prioridad |
|---|---|---|---|---|
| 1 | Manifiesto | `ManifestoScreen.kt` | Onboarding / About | M |
| 2 | Inicio | `HomeScreen.kt` | Tab | ⭐ Alta |
| 3 | Ver (lista) | `SeeTransactionsScreen.kt` | Tab | Alta |
| 4 | Cuentas | `AccountsScreen.kt` | Tab | Media |
| 5 | Nueva cuenta | `AddAccountScreen.kt` | Push form | Media |
| 6 | Perfil | `ProfileScreen.kt` | Tab | Media |
| 7 | Privacidad | `PrivacyPolicyScreen.kt` | Push static | Baja |
| 8 | Reporte | `ReportScreen.kt` | Push | ⭐⭐ Crítica |
| 9 | Nuevo movimiento | `AddTransactionScreen.kt` | Push form | ⭐⭐ Crítica |
| 10 | Editar movimiento | `EditTransaction.kt` | Push form | Alta |
| 11 | Categorías | `CategoriesScreen.kt` | Push | Media |
| 12 | Nueva categoría | `AddCategoryScreen.kt` | Push form | Media |
| 13 | Seleccionar categoría | `SelectCategoryScreen.kt` | Push picker | Alta |

⭐⭐ Crítica = pantalla que define la experiencia del producto. Si está
mal diseñada, el producto no funciona.
⭐ Alta = pantalla muy usada o muy visible.

---

## 12 · Contacto

- **Edgardo** (founder + dev) — edgardo.emm20@gmail.com / WhatsApp
- Repo: privado (te paso APK, no necesitas el código).
- Tiempo de respuesta: <24h para cualquier pregunta de producto.

---

*Última actualización: 2026-05-19. Este doc puede cambiar si vos
descubrís contradicciones — avisame y lo ajustamos.*
