# PLAN_REDESIGN — Notion-Dark visual refactor

Status: **executing** · Started 2026-05-19 · Tag: `pre-redesign`

Refactor visual completo basado en el handoff de Claude Design ("JustChill — Notion Dark", entregado 2026-05-19). Mantiene 100% de la arquitectura actual (Clean + MVI + Koin + SQLDelight). **Sólo toca la capa Compose.**

Fuente del diseño: `claude-design/UYnMaIj0IiFLysbM7mt5MQ` (19 pantallas en 7 secciones · 8 componentes base · 3 bottom sheets · 3 empty states).

## Decisiones tomadas

| # | Decisión | Razón |
|---|---|---|
| 1 | **Hit targets 48dp** (no 44 del mock) | Material Design mínimo. Bumpear IconBtn, NumKey, MonthSelector arrows. |
| 2 | **"+" plano accent en BottomBar** (no FAB) | Mantener la decisión del diseñador para la primera pasada. Revisar después de SR-2 en dispositivo real. |
| 3 | **Manifiesto reemplaza sólo la primera card de onboarding** | El onboarding actual tiene varios pasos. Lo demás se mantiene. |
| 4 | **Inter + IBM Plex Mono** bundleados en `res/font/` | Sin Google Fonts downloadable (offline-first). Lato sale del proyecto. |
| 5 | **SelectCategoryScreen → ModalBottomSheet** | Replace push completo. SR-3 entrega el sheet, SR-8 deprecia la pantalla. |
| 6 | **Light mode pospuesto** | No bloquea alpha. Refactor preserva `values-night` split listo para fase posterior. |
| 7 | **Feature-por-feature en `trunk`** | Cada SR es un commit. Permite iterar visualmente con dev APK entre sprints. |

## Stack mapping (diseño → Compose)

| Diseño (JSX) | Implementación Compose |
|---|---|
| `JC.bg/panel/surface/surface2/hairline/hairline2/text/text2/text3/text4/accent/accentDim/pos/posDim/neg/negDim/cat.*` | Reescribir `EmmColors` en `core/theme/EmmColors.kt` con los hex exactos del diseño |
| Inter UI · IBM Plex Mono (`tnum`) | Agregar `res/font/inter_{400,500,600,700}.ttf` + `ibm_plex_mono_{400,500,600}.ttf`. Actualizar `EmmType.kt` para que **mono** se aplique sólo a `amount*`; el resto Inter. |
| Radii: 8, 10, 12, 14, 16 | Ampliar `EmmRadii` (rXS=8, rS=10, rM=12, rL=14, rXL=16, rFull=999) |
| `JCFrame` + statusbar fake | **Descartar** (era para el canvas) |
| `BottomBar` | `Hh.kt` `Scaffold(bottomBar=…)` rediseñado |
| `MonthSelector` | `core/ui/MonthSelector.kt` |
| `Eyebrow`, `Pill`, `IconTile`, `MetaRow`, `TopBar`, `IconBtn`, `StickyCTA`, `AmountHero`, `MoneyInline`, `Hairline` | nuevos composables en `core/ui/atoms/` |
| `Sheet` + `SheetAccount/Date/Category` | `androidx.compose.material3.ModalBottomSheet` |
| `NumKey` 3×4 numpad | `core/ui/Numpad.kt` |
| Íconos Phosphor-ish (paths SVG inline) | `androidx.compose.material.icons.outlined` cuando matchee. Para los custom (numpad bksp, sparkle, shield, receipt) → `ImageVector` propios en `core/ui/icons/JcIcons.kt`. |

## Sprints

| ID | Scope | Visible en device |
|---|---|---|
| **SR-1** | Foundations: colors + fonts + radii + 10 atoms | No (sólo compila) |
| **SR-2** | Home + BottomBar | ✅ primer "wow" |
| **SR-3** | Add transaction · numpad · 3 sheets | ✅ ritual diario nuevo |
| **SR-4** | Edit transaction | ✅ |
| **SR-5** | Reporte + share | ✅ |
| **SR-6** | Ver | ✅ |
| **SR-7** | Cuentas + Nueva cuenta | ✅ |
| **SR-8** | Categorías + Nueva categoría + Seleccionar (sheet) | ✅ |
| **SR-9** | Perfil + Privacidad + Manifiesto | ✅ fin |

### Iteración

Después de cada SR (excepto SR-1) commit a `trunk`, `assembleDevDebug`, instalar en dispositivo. Edgardo revisa, si hay ajuste menor → micro-commit; si hay ajuste estructural → se anota acá y se hace en una pasada SR-10 polish.

## Risks

- **Inter/IBM Plex Mono no quepan en el APK** — Inter Variable + Plex Mono full pesan ~700 KB. Aceptable. Si presiona el size budget, downsamplear a 400/500/600 únicamente.
- **ModalBottomSheet Material3 + edge-to-edge** — los sheets en Material3 1.4+ tienen friction con WindowInsets. Si falla, fallback a `Dialog` con animación bottom.
- **Tests de UI rotos** — los `androidTest/` que asseren strings/IDs sobreviven; los que asseren layouts visuales pueden romperse. Plan: deshabilitar los que rompen y reescribirlos en SR-10.
- **Onboarding** — si el Manifiesto reemplaza más de lo que esperaba el flujo actual, SR-9 puede revelar regresión en el wizard. Mitigación: leer `onboarding/` antes de SR-9.

## Rollback

Tag `pre-redesign` apunta a `trunk` antes de empezar (commit `8593c1d`).

- Rollback total: `git reset --hard pre-redesign` y borrar `docs/PLAN_REDESIGN.md`.
- Rollback parcial por sprint: cada SR es 1 commit, `git revert <sha>` lo deshace.

## Estado actual de ejecución

- [x] SR-0 plan
- [ ] SR-1 foundations
- [ ] SR-2 home
- [ ] SR-3 add
- [ ] SR-4 edit
- [ ] SR-5 reporte
- [ ] SR-6 ver
- [ ] SR-7 cuentas
- [ ] SR-8 categorías
- [ ] SR-9 perfil/privacidad/manifiesto

## Notas para el ejecutor (Sonnet)

Cada SR es un prompt cerrado. Reglas globales:

1. **No tocar arquitectura.** ViewModels, UseCases, Repositories, SQLDelight quedan intactos. Sólo capa Compose + tokens.
2. **No agregar features.** Si el diseño muestra un control que hoy no existe (ej. swipe-to-delete), anotarlo como TODO en el commit, no implementar.
3. **Hex literales del diseño** sólo en `EmmColors.kt`. Resto del código consume tokens vía `LocalEmmColors.current.<token>`.
4. **Tabular nums** `fontFeatureSettings = "tnum"` obligatorio en montos.
5. **48dp** mínimo para cualquier tap target. Usar `Modifier.minimumInteractiveComponentSize()` o `.size(48.dp)`.
6. **No crear archivos extra** salvo los listados en cada SR.
7. **Build verde** antes de commitear: `./gradlew assembleDevDebug` debe compilar limpio.
8. **Commit format**: `feat(redesign-srN): <scope> — <one-line summary>` (sin Co-Authored-By).
