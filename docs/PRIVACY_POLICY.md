# Política de privacidad — JustChill

**Última actualización**: 2026-08-08

JustChill es una app de finanzas personales que funciona primero en tu
celular. Esta política explica qué información tratamos, cuándo sale de
tu celular y cómo la borras.

## Tu data vive en tu celular

Sin cuenta, todo —cuentas, categorías, movimientos— se guarda solo en
tu celular. No necesitas registrarte para usar la app y no mandamos
nada a ningún servidor.

## Si creas una cuenta (opcional)

Puedes crear una cuenta para sincronizar tu data entre tus
dispositivos, de dos maneras: con tu correo y una contraseña, o con tu
cuenta de Google. Es opcional: la app completa funciona sin cuenta.

Si te registras, guardamos en nuestros servidores (Supabase):

- **Tu correo**, para identificar tu cuenta. Si usas correo y
  contraseña, la contraseña se guarda como hash — ni nosotros podemos
  leerla.
- **Tu data financiera**: cuentas, categorías, movimientos y
  movimientos recurrentes — montos, descripciones y fechas —
  asociados a tu cuenta.

### Si entras con Google

Cuando eliges "Continuar con Google", tu celular le pide a Google que
confirme quién eres. Google nos devuelve un token firmado que incluye
tu correo y los datos básicos de perfil de la cuenta que elegiste.
Usamos ese token únicamente para crear o abrir tu cuenta; de todo eso
guardamos solo tu correo, igual que en el registro con contraseña.

Nunca recibimos tu contraseña de Google ni tenemos acceso a tu Gmail,
tus contactos ni ningún otro servicio de Google. El intercambio lo
maneja Google en tu propio celular, así que Google sabe que iniciaste
sesión en JustChill; lo que hagas dentro de la app no se le informa.

Esa data viaja cifrada (HTTPS) y cada cuenta solo puede acceder a su
propia data. No la vendemos, no la compartimos y no la usamos para
nada que no sea sincronizar tus dispositivos.

## Borrar data y borrar tu cuenta

- **Borrar un movimiento**: desaparece de tu celular y el borrado se
  propaga a tus otros dispositivos. En el servidor queda una marca de
  borrado (necesaria para esa propagación) hasta que borres tu cuenta.
- **Cerrar sesión**: tu data local se queda en tu celular. El servidor
  conserva lo ya sincronizado para cuando vuelvas a entrar.
- **Borrar tu cuenta**: puedes hacerlo directamente desde la app en
  Perfil → "Eliminar cuenta". Se elimina tu cuenta de Supabase y toda
  tu data del servidor de forma inmediata; tu data local sigue en tu
  celular. También puedes escribirnos a edgardo.emm20@gmail.com desde
  el correo de tu cuenta y lo hacemos nosotros dentro de 30 días.

## Exportar tu data

Si exportas tu data a un archivo, tú decides qué hacer con él —
guardarlo, mandarlo o borrarlo. Nosotros no recibimos copia.

## No usamos analytics

No usamos cookies ni analytics. No sabemos qué pantallas abres ni
cuántos movimientos registras.

## Crashlytics (solo en la versión publicada)

La versión publicada en Play Store usa Firebase Crashlytics
exclusivamente para reportar crashes (errores que tumban la app).
Esto envía a Google: el modelo de tu celular, la versión de Android,
y el stack trace del error. **Nunca enviamos tu data financiera.**

Si quieres una versión sin Crashlytics, puedes compilar la app desde
el código fuente con el flavor `dev`.

## Si reinstalas

- **Sin cuenta**: si reinstalas la app o cambias de celular sin
  exportar primero, la data se pierde.
- **Con cuenta**: inicia sesión y tu data sincronizada vuelve.

## Contacto

Edgardo Muñoz — edgardo.emm20@gmail.com

---

**Para hostear esta política como URL pública** (requerido por Play
Store; la sección "Borrar data y borrar tu cuenta" sirve también como
el recurso web de eliminación de cuenta que pide el Data Safety form):

1. Crear un Gist público en https://gist.github.com con este markdown.
2. Copiar la URL del Gist.
3. Pegarla en Play Console → Privacy Policy URL y en Data Safety →
   account deletion URL.

Alternativa: subir el markdown como page en GitHub Pages del repo.
