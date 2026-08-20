# PRODUCT REQUIREMENTS — límites de alcance y criterios no funcionales

Este doc no describe lo construido: eso lo describe el código. Quedan las dos cosas que el
código no puede mostrar — **qué está fuera de alcance** y **contra qué criterios se juzga lo
que entra**.

**Los ids de fila de §1 son inmutables**: los ADRs enmiendan este doc citándolos (ADR 001 →
W-02 / W-03 / W-11; ADR 009 → W-04, W-08). Renumerar rompe esas referencias.

---

## 1. Won't have — explícitamente fuera de alcance

Estas no se construyen. Si una se mueve a alcance, se renegocia el positioning primero y la
decisión se firma en un ADR.

| ID | No se hace | Razón |
|---|---|---|
| W-01 | **Conexión al banco / bank-sync** | La app es de captura manual; el bank-sync arrastra permisos, credenciales y soporte que un side project unipersonal no sostiene. |
| W-04 | **Multi-cuenta compartida (parejas, equipos)** | El producto es individual. ADR 009 se apoya en esta fila: sin dos escritores concurrentes, la replicación fila a fila no se paga. |
| W-05 | **Presupuestos rígidos ("no gastes más de X")** | La app observa, no vigila. |
| W-06 | **Multi-moneda** | Solo soles. |
| W-07 | **Cursos / lecciones de educación financiera** | No es una app de contenido. |
| W-08 | **Notificaciones push diarias / gamificación / badges** | Anti-complejidad. ADR 009 se apoya en esta fila: no hay feature que exija un servidor que lea las filas. |
| W-09 | **Suscripción premium / paywall** | Gratis, sin paywall. Un muro sobre features básicas es traición al posicionamiento. |
| W-10 | **Anuncios** | Sin ads. La declaración de advertising ID en Play Console tiene que decir "No" (`PLAY_ADVERTISING_ID.md`). |
| W-12 | **OCR / lectura automática de notificaciones Yape** | Se eligió el mensaje radical (manual, 30 segundos) por sobre la integración audaz. |

### Revisados como opcionales opt-in (ADR 001)

Estos tres eran Won't. Siguen siendo **opt-in**: la app funciona 100% sin ellos, sin cuenta y
sin red, y el estado "sin cuenta" es permanente y soportado.

| ID | Decisión original | Estado |
|---|---|---|
| W-02 | **Cloud backend propio** | **Opt-in.** Supabase solo se usa si el usuario activa el respaldo desde Perfil (ADR 009 reemplaza "sync" por "respaldo" sin tocar la fila). |
| W-03 | **Google Sign-In / cuenta** | **Opcional.** Cuenta email/password o Google Sign-In, únicamente si el usuario quiere respaldo. |
| W-11 | **Sync entre dispositivos** | **Opt-in**, y ADR 009 lo redujo a snapshot backup: no hay replicación fila a fila ni convergencia multi-device. |

---

## 2. Criterios no funcionales

| Área | Requisito |
|---|---|
| **Arranque** | <3s primer launch (cold), <1s warm, en gama media. |
| **Registro de un movimiento** | <15s extremo a extremo para el usuario habitual. Cualquier cambio que sume un tap al flujo principal se rechaza. |
| **Tamaño del APK** | Objetivo <15MB, hard limit 25MB. |
| **Mínimo Android** | `minSdk` no es uniforme entre módulos, y eso es deliberado: la fuente es `build-logic/.../BuildConventions.kt`. |
| **Idioma** | Solo español. Sin fallback a inglés, sin selector. |
| **Accesibilidad** | Texto escalable sin breakage, contraste WCAG AA, content descriptions en elementos interactivos. Screen reader completo no está comprometido. |
| **Offline** | 100% funcional sin red para lectura y escritura. **Cero requests HTTP sin consentimiento explícito**: sin cuenta, la app no contacta ningún servidor. |
| **Persistencia** | SQLDelight local es la única fuente de verdad. Un crash o force-close no pierde data. Toda migración preserva la data del usuario. |
| **Batería** | Sin WorkManager periódico ni servicios en segundo plano. Con sesión activa, el respaldo corre en background/resume; sin sesión, cero actividad de red. |
| **Crashes** | Telemetría solo en el flavor `prod`. El flavor `dev` no reporta (`androidApp/CLAUDE.md`). |

---

## 3. Criterio de aceptación

Una feature entra solo si cumple **todo** esto:

- No está en §1.
- Le ahorra tiempo al usuario o le aporta claridad. "Queda bonito" y "todas las apps lo tienen"
  no son razones.
- No reduce la velocidad de registro de un movimiento.
- Es descubrible: no requiere onboarding ni tutorial para encontrarse.
- No pide data del usuario que la app no necesita para funcionar.
