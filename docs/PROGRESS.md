# JustChill — Progreso v1

> Estado del proyecto a fecha del último update. Punto de re-entrada
> para retomar después de cerrar/limpiar el contexto.
>
> **Última actualización**: 2026-05-18.

---

## TL;DR — dónde estamos ahora

- **Proceso de definición**: ✅ Fases 1-5 firmadas y versionadas.
- **Ejecución**: Sprint 0 (quick wins) cerrado. Sprint 1 (Reporte de
  ingresos) con código listo + tests verdes; **falta verificación
  manual en device** antes de cerrar y tagear `post-s1`.
- **Próximo paso concreto**: instalar `./gradlew installDevDebug`,
  validar los 5 checks de la sección "Verificación manual S1" abajo,
  y si todo va — tagear `post-s1` y arrancar Sprint 2 (Onboarding).

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
| 3. Architecture Review | `ARCHITECTURE_REVIEW.md` | ✅ Firmada |
| 4. Roadmap v1 | `ROADMAP_V1.md` | ✅ Firmada |
| 5. Post-v1 plan | `POST_V1_PLAN.md` | ✅ Firmada |

Decisiones bloqueadas (no se renegocian sin volver a Fase 1):
- Target: peruano 25-35 con sueldo + ingresos extras.
- 100% local, sin login, sin cloud backend.
- Gratis sin paywall en v1.
- Pilar diferencial: claridad de ingresos múltiples + simplicidad.
- Manifesto provocador-minimalista, enemigo: la complejidad innecesaria.

---

## Roadmap de sprints (8 totales, ~10 semanas calendario)

| Sprint | Foco | Estado | Tag |
|---|---|---|---|
| **S0** | 9 quick wins (cleanup) | ✅ Completo | `post-s0` |
| **S1** | Reporte ingresos + nav meses (US-11, US-09) | ⚠️ Code listo, falta verif device | — |
| **S2** | Onboarding + manifiesto (US-01, US-02) | ⏳ Pendiente | — |
| **S3** | Export/Import JSON (US-18, US-19) | ⏳ Pendiente | — |
| **S4** | ProfileScreen completo (US-21) | ⏳ Pendiente | — |
| **S5** | Polish + accesibilidad + screenshots | ⏳ Pendiente | — |
| **S6** | Dogfooding propio intensivo | ⏳ Pendiente | — |
| **S7** | Testers externos + publicación alpha | ⏳ Pendiente | — |

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

## Rollback points (tags git)

| Tag | Cuándo | Comando para volver |
|---|---|---|
| `pre-s0` | Antes de ejecutar — Fases 1-5 firmadas + DESIGN_SYSTEM alineado | `git reset --hard pre-s0` |
| `post-s0` | 9 quick wins completados, repo limpio | `git reset --hard post-s0` |

**Próximo tag esperado**: `post-s1` (después de verificación manual).

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
- Si el user pide arrancar un sprint, **leer el plan correspondiente
  en `ROADMAP_V1.md` primero** para no inventar nada.
