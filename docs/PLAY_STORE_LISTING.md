# Play Store listing — JustChill

> Copy listo para pegar en Google Play Console (alpha cerrada).
> Normalizado a **tú** (peruano), coherente con el manifesto in-app
> post-S5 y la política de privacidad.
>
> Escrito para la 2.0.0-alpha. El copy de la ficha sigue vigente; la sección
> "What's new" es específica de esa versión y hay que reescribirla en cada
> release (el tag más reciente es `v2.2.0`). El `versionName` sale del último
> tag de git, no de este archivo.

---

## Título de la app (límite 30 caracteres)

```
JustChill — plata clara
```

(24 chars. Si prefieres más punchy y direccional: `"JustChill: cuánto entra y sale"` — exactamente 30, cero margen.)

## Descripción corta (límite 80 caracteres)

```
Sin login. Sin servidor. Sabe cuánto entra y sale, en 30 segundos al día.
```

(73 chars.)

## Descripción larga (límite 4000 caracteres)

```
Tu plata vive en tu celular. No la mandamos a ningún servidor.

JustChill es una app para anotar cuánto entra y cuánto sale, sin hacerse la complicada.

QUÉ HACE:
• Anotas tus ingresos y gastos en pocos taps.
• Ves un reporte de qué ingresos te entran este mes — el sueldo, lo del freelance, las propinas, las ventas. Cada fuente clara.
• Comparas con el mes pasado. Sin dashboards, sin gráficos raros.
• Mensual, en soles, listo.

QUÉ NO HACE (y no lo va a hacer):
• No te pide login.
• No se conecta a tu banco.
• No te manda notificaciones diarias.
• No te vende premium ni te muestra anuncios.
• No te sincroniza con la nube.
• No tiene "presupuestos" que te policíen.
• No te enseña educación financiera porque no la necesitas — ya eres adulto.

PARA QUIÉN:
Para el que tiene un sueldo pero también vende cosas por Instagram, hace consultoría freelance, recibe propinas o tiene ingresos que cambian mes a mes. Para el que quiere SABER cuánto realmente entra, no que le pidan ser "más disciplinado".

TU PRIVACIDAD:
Todo vive en tu celular. Sin servidor. Sin cookies. Sin analytics de tu uso. Si quieres respaldar tu data, exportas un JSON y lo guardas tú donde quieras.

Si reinstalas la app sin exportar primero, la data se pierde. Es el precio de no tener servidor — y nos parece justo.

JustChill es para gente que ya está cansada de las apps que tratan a su billetera como un proyecto de productividad. Solo tú, tu plata, y la verdad.
```

(~1700 caracteres.)

## What's new — versión 2.0.0-alpha

```
Reescritura completa: ahora 100% local. Sin login, sin nube, sin sync.
Tu data vive solo en tu celular. Puedes exportar/importar JSON cuando
quieras. Manifesto al primer launch, nueva pantalla de Perfil con
ajustes, reporte de ingresos por fuente, edit/borrar cuentas y
categorías, atajo "Volver a hoy" en navegación de meses.
```

## Categoría sugerida

- Categoría principal: **Finanzas**
- Categoría secundaria: ninguna

## Etiquetas / tags

`finanzas personales`, `presupuesto`, `Perú`, `soles`, `ingresos`,
`gastos`, `sin internet`, `local`, `privado`

## Configuración del listing en Console

| Campo | Valor |
|---|---|
| Idioma principal | `es-419` (Español Latinoamérica) |
| Contiene anuncios | No |
| Compras dentro de la app | No |
| Acceso restringido | No |
| URL política de privacidad | (pendiente — ver `PRIVACY_POLICY.md` y subir como Gist público) |
| Email de contacto | edgardo.emm20@gmail.com |
| Audiencia objetivo | 18+ |
| Distribución geográfica inicial | Perú (expandir Latam si pedidos lo justifican) |

## Screenshots requeridos

Play Store alpha mínimo: **2 capturas por dispositivo**.
- 4 pantallas a fotografiar: Home, Report, AddTransaction, Profile.
- Formato recomendado: 1080×1920 (FHD vertical).
- Formato alternativo aceptado: 1080×2400 (algunos celulares modernos).

Si no las tomaste durante S5 device-verif, puedes generarlas con:

```bash
# 1. abrir cada pantalla en el emulador / device
adb shell screencap -p /sdcard/sc.png && adb pull /sdcard/sc.png home1.png
```

## Ícono de Play Store

Ya existe: `androidApp/src/prod/ic_launcher_first-playstore.png` (512×512).
Usa ese — no hay que regenerar. (El módulo `:app` pasó a llamarse `:androidApp` en la migración KMP;
hay una copia equivalente en `androidApp/src/dev/`.)

## Notas estratégicas

- **Distribución alpha cerrada**: 5-10 testers reclutados directamente.
  No buscar instalación orgánica.
- **No promocionar**: el manifesto es el pitch. Que se contagie por
  WhatsApp, no por SEO.
- **No abrir reseñas públicas** todavía — alpha cerrada las desactiva
  por default.
