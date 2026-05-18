# POST-V1 PLAN — JustChill después del alpha

**Fase**: 5 de 5 (Post-v1 + growth)
**Bloquea**: nada — es la última fase del proceso de definición.
**Depende de**: Fases 1-4 firmadas.

> Este doc define qué pasa **después** de publicar v1 en Play Store
> alpha. Backlog de v2+, estrategia de creators, hipótesis de
> monetización, métricas que deciden el rumbo, y triggers de pivot.
> No es un compromiso — es un mapa para no improvisar cuando
> empiece a haber data real.

---

## 0. Resumen ejecutivo

JustChill **no se monetiza en v1, ni v2**. La pregunta de monetización se
abre solo si llegamos a **1,000+ usuarios activos retenidos a 4 semanas**.
Hasta entonces, es un side project con ambición — gratis, sin paywall, sin
ads. Esa restricción es la **mejor estrategia de marketing** que tenemos:
la app que no te vende nada.

**El embudo a 12 meses**: alpha cerrada (5-10 testers) → beta abierta (~100
usuarios vía outreach a 3-5 creators chicos) → producción (~1,000+ usuarios
si las métricas pegan).

**El verdadero gate de pivot**: si retención semana 4 no llega a ≥15% en
beta, **no es un problema de marketing — es un problema de producto**.
Volvemos a Fase 1.

---

## 1. El embudo post-alpha

```
┌─────────────────────────────────────────────────────────┐
│ V1 ALPHA CERRADA       (sem 10-12 del roadmap)          │
│ Testers: 5-10          Canal: WhatsApp 1-a-1            │
│ Gate de salida: retención 4 sem ≥ 30% en testers        │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ V1 BETA ABIERTA        (mes 4-6 del proyecto)           │
│ Usuarios: ~50-200      Canal: 3-5 creators peruanos     │
│ Gate de salida: retención 4 sem ≥ 20% Y CSF >99%        │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ V1 PRODUCCIÓN          (mes 6-9 del proyecto)           │
│ Usuarios: ~500-2000    Canal: Play Store + creators     │
│ Trigger de v2: feedback consistente + métricas estables │
└───────────────────────────┬─────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│ V2                     (mes 9-15 del proyecto)          │
│ Decidir scope basado en feedback de v1                  │
│ Posible (no firmado): monetización                      │
└─────────────────────────────────────────────────────────┘
```

**No hay timeline rígido para los gates**. Si el alpha de 10 testers tarda
2 meses en juntar data útil, esperamos 2 meses. Mejor lento y honesto que
rápido y a ciegas.

---

## 2. Feature backlog v2 (lo que NO entró en v1 con razón)

Estas features están **explícitamente diferidas**. No son "won't" (esos
están en Fase 2 §5 y NO entran nunca). Son "vamos a pensarlo en v2 si la
data lo justifica".

### Priorizado por ROI (impacto / esfuerzo)

| ID | Feature | Origen | Triggers para construirla en v2 |
|---|---|---|---|
| F-01 | **Tema claro** | Fase 2 §6 (v1 solo dark) | Pedido por >30% de testers. |
| F-02 | **Comparativa de 3-6 meses** | Fase 2 US-13 | Usuario de >3 meses dice "quiero ver tendencia". |
| F-03 | **Reporte de gastos por categoría** | Fase 2 US-12 | Si no entró en v1 Sprint 4. |
| F-04 | **Export CSV (para Excel)** | Fase 2 US-20 | >5 usuarios lo piden explícitamente. |
| F-05 | **Recordatorio gentil opt-in** | Fase 1 §5.5 | Métrica: <40% llegan al reporte mensual. |
| F-06 | **Widget Android del saldo del mes** | Idea | Si testers piden ver saldo sin abrir la app. |
| F-07 | **Búsqueda avanzada** (rango de monto, fecha) | Extensión US-07 | >10% buscan con filtros que no tenemos. |
| F-08 | **Categorías recurrentes / templates** | Idea | Usuario habitual reporta "siempre registro las mismas 3 cosas". |
| F-09 | **Backup automático a Drive del usuario** | Fase 1 §4 opción B | >20% pierde data al cambiar de celu. |
| F-10 | **Onboarding mejorado con data de ejemplo** | Fase 1 §5.5 mitigación | Retención sem 1 < 50%. |

### NO en este backlog (por positioning, no por capacidad)
- Bank-sync, paywall, suscripción, multi-moneda, multi-país,
  notificaciones agresivas, social/compartido, gamificación. Esos son
  los **Won't have** de Fase 2 §5. No reaparecen acá.

---

## 3. Estrategia de outreach a creators peruanos

Fase 1 §3 firmó que la distribución es "orgánica + creators (no Google
Ads, no influencer pagado)". Acá se desarrolla.

### 3.1 Perfil del creator objetivo

- **Tamaño**: 5,000 - 50,000 seguidores. Más chicos no mueven la
  aguja; más grandes son inalcanzables como side project y nos van a
  pedir plata.
- **Vertical**: finanzas personales / emprendimiento / side hustles /
  productividad. NO cripto, NO inversiones, NO "libertad financiera"
  motivacional.
- **Geografía**: Perú. Idealmente Lima/Arequipa/Trujillo (target de
  Sebastián).
- **Tono**: alguien con autoría real, no "guru reciclado". Mejor si
  habla de su propia plata, no solo de tips genéricos.

### 3.2 Lista de candidatos (a construir en S6-S7 del roadmap)

**Acción concreta**: durante los Sprints 6-7 (dogfooding y testers),
dedicar 1-2 horas a buscar en TikTok/Instagram/YouTube creators que
cumplan el perfil. Armar lista de **15-20 candidatos** con:
- Handle / link
- Audiencia aproximada
- 2-3 ejemplos de contenido que demuestre alineamiento
- Email o DM de contacto

Esa lista vive en `docs/CREATORS_LIST.md` (no se hace ahora, se hace
cuando estemos cerca del alpha).

### 3.3 La oferta inicial (qué les decís)

**NO se ofrece plata**. Lo que se ofrece:
- Acceso temprano a la app (alpha cerrada).
- Una historia honesta de un peruano que la construyó para sí mismo.
- Cero pedido de promoción — solo "probala si te interesa, si te sirve
  contala como quieras o no la contás".

**Si querés decírselo a uno, el mensaje base es ~150 palabras**:

> "Hola [nombre]. Soy peruano, construí una app de finanzas porque
> no encontraba ninguna que entienda al freelance limeño con sueldo
> + ingresos extras en soles. Es 100% local — no pide cuenta, no
> manda data a ningún lado, no te vende premium. Está en alpha
> cerrada con 5 testers. Te quería mandar acceso, no para que la
> recomiendes ni nada — solo si te interesa probarla porque encaja
> con lo que contás de finanzas personales. Si funciona, perfecto;
> si no, también perfecto. ¿Te paso el link?"

### 3.4 Métricas de éxito del outreach

- **Phase 1** (S7 del roadmap): 3-5 creators con acceso al alpha.
- **Phase 2** (mes 4-5): 1-2 mencionan la app orgánicamente (sin pedido).
- **Phase 3** (mes 6+): si UN solo creator hace un video genuino → eso
  típicamente trae 500-5,000 instalaciones según su tamaño.

**Lo que NO hacemos jamás**:
- Pagar por menciones.
- Pedir que digan algo específico ("decí que es la mejor").
- Mandar 50 DMs templateados — la outreach es 1-a-1 con personalización
  real.

---

## 4. Hipótesis de monetización (4 caminos posibles)

Fase 1 §1 dijo "no me importa todavía" + "pero se DISEÑA pensando en
monetización futura". Hoy listamos las opciones para tenerlas mapeadas,
sin firmar ninguna.

### 4.1 Camino A — "Nunca" (totalmente gratis para siempre)

- **Pros**: máximo alineamiento con manifiesto. Diferenciación pura.
- **Contras**: no sostenible si crece a costos reales (Play Store fee,
  Crashlytics, dominio, tiempo). El proyecto muere o vos lo
  sostenés de tu bolsillo.
- **Cuándo**: si nunca pasa de ~500 usuarios. Es un side project bonito
  que vive en un rincón.

### 4.2 Camino B — Donación (pay what you want)

- **Pros**: respeta el espíritu. Algunos usuarios contentos donan, el
  resto no paga nada. Cero fricción.
- **Contras**: ingresos mínimos. Funciona para apps con 50k+ usuarios.
- **Cuándo**: si llegamos a 5k+ usuarios y querés que la app financie
  sus costos.

### 4.3 Camino C — Premium tier con UNA feature (no muro)

- **Pros**: alineado parcialmente con manifiesto si el premium es UNA
  feature de poder, no un muro a features básicas.
- **Contras**: empieza el feature creep. Cada usuario premium pide
  más.
- **Candidatos a feature premium** (a discutir en v2):
  - Backup automático a Drive del usuario (F-09).
  - Reportes anuales con análisis profundo.
  - Tema personalizado / múltiples temas.
- **Cuándo**: si llegamos a 10k+ usuarios y >5% pide algo similar.
- **Cómo NO hacerlo**: paywall sobre categorías custom, presupuestos,
  notificaciones — todo eso es traición (Fase 2 §5 W-09 lo prohíbe).

### 4.4 Camino D — Vender el código / open source con donaciones

- **Pros**: máximo alineamiento. Te liberás de mantenerla solo.
- **Contras**: perdés control de la marca. Bifurcación posible.
- **Cuándo**: si después de 2 años decidís que ya no querés
  mantenerla pero querés que sobreviva.

### 4.5 Recomendación de hoy
**Esperamos hasta que haya métricas reales.** No tomamos ninguna decisión
hasta que la app tenga >500 usuarios activos. Si llegamos a eso, hablamos.

### 4.6 Implicancias técnicas en v1 para no cerrarnos puertas
- **Schema JSON exportable**: ya está con `schemaVersion`, permite
  migrar a futuro. ✅
- **NO meter campos "isPremium" en schema v1**. Si los necesitamos, los
  agregamos en v2 con migración. Mejor schema limpio hoy.
- **Mantener Crashlytics solo en `prod`**: ya está. Sirve para cuando
  haya escala.

---

## 5. Métricas que deciden el rumbo

### Después del alpha (mes 3 desde publicación)

| Métrica | Bueno | Aceptable | Problema |
|---|---|---|---|
| Retención sem 4 (testers) | ≥40% | 25-40% | <25% |
| Crash-free sessions | >99.5% | 99-99.5% | <99% |
| % llegan al primer reporte mensual | >50% | 30-50% | <30% |
| Feedback espontáneo positivo | >2 testers | 1 tester | 0 testers |
| Bugs críticos reportados | 0 | 1-2 | 3+ |

### Después de la beta (mes 6 desde publicación)

| Métrica | Bueno | Aceptable | Problema |
|---|---|---|---|
| Instalaciones acumuladas | >500 | 100-500 | <100 |
| Retención sem 4 | >20% | 15-20% | <15% |
| Reviews orgánicas Play Store | ≥4.0 estrellas | 3.5-4.0 | <3.5 |
| Menciones orgánicas en RRSS | 2-3+ | 1 | 0 |

### Si la beta termina en "Problema" en 2+ métricas
**Volvemos a Fase 1**. No se parchea, no se mete más features, no se
intenta marketing. Se repensa el producto. Esa es la disciplina.

---

## 6. Triggers de pivot (cuándo volver a Fase 1)

Tres triggers concretos:

1. **Retención sem 4 < 15% en beta abierta con 100+ usuarios**.
   Significa que el producto no engancha. El reporte mensual no es el
   ajá moment que pensábamos, o el ritual nocturno no funciona, o
   simplemente no es la app que Sebastián necesita.
2. **Feedback consistente pidiendo algo que está en "Won't have"**
   (>30% pide bank-sync, o paywall, o multi-cuenta compartida).
   Significa que el positioning leyó mal al target.
3. **0 menciones orgánicas en RRSS después de 5+ creators con
   acceso** durante 3+ meses. Significa que la historia no es contable
   — el manifesto suena bonito pero no se traduce a algo que un creator
   quiera compartir.

**Cuando se dispara un trigger**, el orden es:
1. Hablar con 5-10 usuarios reales (no asumir).
2. Volver a Fase 1 §1-3 y reevaluar persona / positioning.
3. Decidir: ajustar (cambio menor en Fase 1) o pivotar (cambio fuerte
   que rompe varias decisiones).
4. Re-firmar lo que cambia. NO seguir construyendo encima de un
   positioning roto.

---

## 7. Política de feature requests (la disciplina)

Fase 1 §6 riesgo #7 dice: *"¿La postura 'anti-complejidad' no se diluye
en v2? Apple, Notion y Spotify empezaron simples y se llenaron."*

Para no caer en la trampa, **toda feature request post-v1 se evalúa
contra esta checklist**:

### Una feature ENTRA al backlog v2+ solo si cumple TODO esto:
- [ ] No está en los 12 Won't have de Fase 2 §5.
- [ ] Tiene trazabilidad clara con alguna decisión de Fase 1 (no
      contradice positioning).
- [ ] >5 usuarios la pidieron explícitamente (no inventada por mí).
- [ ] Se puede construir sin agregar >1 dependencia nueva.
- [ ] Tiene un "ajá moment" claro (sin esto, ¿qué problema resuelve?).
- [ ] NO requiere onboarding ni tutorial — debe ser descubrible.

### Una feature se RECHAZA explícitamente si:
- Se justifica con "todas las apps lo tienen".
- Se justifica con "queda bonito".
- Se justifica con "mi tío lo pidió" (n=1 no es feedback).
- Reduce velocidad de registro de movimiento (cualquier cosa que sume
  un tap al flujo principal).
- Agrega notificaciones push.
- Pide más data del usuario que la app no necesita para funcionar.

---

## 8. Roadmap calendario de muy alto nivel

> NO son fechas comprometidas. Son anchors para no perder dirección.

| Período | Fase | Foco |
|---|---|---|
| Mes 1-3 (2026-05 a 2026-07) | V1 construcción | Roadmap Fase 4 — Sprints S0-S7 |
| Mes 4-5 (2026-08 a 2026-09) | V1 alpha | Testers, ajustes basados en feedback |
| Mes 6-7 (2026-10 a 2026-11) | V1 beta | Outreach a 3-5 creators, 50-200 usuarios |
| Mes 8-9 (2026-12 a 2027-01) | V1 producción | Si métricas pegan, abrir Play Store completo |
| Mes 10-12 (2027-02 a 2027-04) | Decisión v2 | Revisar feedback, priorizar 2-3 features de F-01..F-10 |
| Mes 13+ (2027-05+) | V2 construcción | Solo si v1 produccion validó el producto |

---

## 9. Riesgos de largo plazo

| Riesgo | Cuándo aparece | Mitigación |
|---|---|---|
| Vos perdés interés / motivación | Mes 6-12 | Si pasa, abrir source (camino D). Mejor que muera publicada. |
| Otra app peruana hace lo mismo primero | Cualquier momento | Diferenciación está en marca + manifiesto, no en feature exclusiva. Lo "primero" pesa menos que lo "mejor contado". |
| Play Store cambia política de privacy | Mes 6+ | Como la app no manda nada a servidores, somos compliant con casi cualquier política. |
| Yape/Plin lanzan su propia app de tracking | Cualquier momento | Improbable a corto plazo. Si pasa, nuestro nicho de "manual + multi-cuenta" sigue válido. |
| Crashlytics se vuelve de pago / caro | Mes 12+ | Migrar a Sentry self-hosted o quitar telemetría. Decisión post-v1. |

---

## 10. Lo que NO está en este doc (y por qué)

- **Estrategia internacional**: si JustChill funciona en Perú, la
  pregunta "¿LatAm? ¿España?" se abre en v3+. Pero requiere romper
  Won't W-06 (multi-moneda) y W-04 (geo Perú) — eso es renegociar
  Fase 1, no extender este doc.
- **Equipo / cofounders**: este es un side project unipersonal. Si en
  algún momento sumás gente, este doc cambia de tono completo.
- **Branding visual avanzado** (logo evolution, brand book): si llega
  el momento (probablemente cuando haya 5k+ usuarios), se hace aparte.

---

## 11. Chequeo final — preguntas para vos

Las últimas 4. Después de esto, **todo el proceso de definición está
cerrado** y empezamos a codear.

1. **El embudo a 12 meses**: ¿se siente realista, o muy optimista, o
   muy modesto? Algunos lo ven y dicen "no llego a 500 usuarios ni en
   dos años" y otros dicen "yo apunto a 50,000". Decime tu honestidad.
2. **Monetización**: los 4 caminos (sección 4) — ¿alguno te suena
   "ese no, jamás"? Si Camino C te parece traición ya, lo sacamos de
   la lista. Si Camino A te parece infantil, también. Mejor saberlo.
3. **Política de feature requests** (sección 7): ¿la firmás como
   está? Especialmente la regla de "se rechaza si reduce velocidad de
   registro" — esa es brutal, pero es la disciplina que mantiene a
   JustChill siendo JustChill.
4. **Triggers de pivot**: ¿la idea de "volver a Fase 1" te suena
   constructiva o asusta? Si asusta, hablamos. Pivot no es fracaso —
   es la única forma honesta de no construir sobre arena.

Si las 4 son "sí" → firmamos Fase 5 y **arrancamos S0 (los 9 quick
wins) ya**.
Si alguna es "no" → ajustamos.

---

## 12. Resumen del proceso completo

Vos partiste con: *"siento que mi app aún no está lista, es simplona,
hay miles parecidas y mejores, qué de nuevo estoy haciendo T.T"*.

5 fases después, lo que existe:

| Fase | Doc | Decisión central |
|---|---|---|
| 1 | `PRODUCT_DISCOVERY.md` | Sebastián, peruano 25-35 con sueldo + ingresos extras. Manifesto anti-complejidad. |
| 2 | `PRODUCT_REQUIREMENTS.md` | 15 Must, 12 Won't explícitos, métrica norte retención sem 4. |
| 3 | `ARCHITECTURE_REVIEW.md` | 9 quick wins, 5 big rocks, ~1,600 LOC para llegar. |
| 4 | `ROADMAP_V1.md` | 10 semanas, 8 sprints, gate pre-alpha. |
| 5 | `POST_V1_PLAN.md` | Embudo 12 meses, 4 caminos de monetización, triggers de pivot. |

JustChill ya no es "una app simplona más". Es una apuesta deliberada
contra la complejidad, con un nicho claro, una historia contable, un
plan calendarizado y una disciplina de cómo decir que no.

**Lo que falta ya no es definir. Es ejecutar.**
