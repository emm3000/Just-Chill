# Política de privacidad — JustChill

**Última actualización**: 2026-08-20

JustChill es una app de finanzas personales que funciona primero en tu
celular. Esta política explica qué información tratamos, cuándo sale de
tu celular y cómo la borras.

## Tu data vive en tu celular

Tus cuentas, categorías, movimientos y movimientos recurrentes se
guardan solo en tu celular. No necesitas registrarte para usar la app,
y hoy tu data financiera no sale de tu celular ni siquiera si creas una
cuenta.

## Si creas una cuenta (opcional)

Puedes crear una cuenta de dos maneras: con tu correo y una
contraseña, o con tu cuenta de Google. Es opcional: la app completa
funciona sin cuenta.

Si te registras, en nuestros servidores (Supabase) guardamos **tu
correo**, para identificar tu cuenta, y —si te registras con correo y
contraseña— el hash de esa contraseña, que ni nosotros podemos leer.
Nada más.

Tu data financiera —montos, descripciones, fechas— **no se sube a
nuestros servidores**: hoy tener cuenta no la respalda ni la sincroniza
entre dispositivos.

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

Tu correo viaja cifrado (HTTPS) y cada cuenta solo puede acceder a lo
suyo. No lo vendemos, no lo compartimos y no lo usamos para nada que no
sea identificar tu cuenta.

## Borrar data y borrar tu cuenta

- **Borrar un movimiento**: deja de existir para la app; en la base de
  datos de tu celular queda una marca de borrado. Como hoy no subimos
  tus movimientos, no queda copia nueva en el servidor.
- **Cerrar sesión**: tu data local se queda completa en tu celular. No
  se borra nada al cerrar sesión ni al entrar con otra cuenta.
- **Borrar tu cuenta**: puedes hacerlo directamente desde la app en
  Perfil → "Eliminar cuenta". Se eliminan de forma inmediata tu cuenta
  de Supabase —con ella, tu correo— y cualquier fila que te pertenezca
  en el servidor; tu data local sigue intacta en tu celular. También
  puedes escribirnos a edgardo.emm20@gmail.com
  desde el correo de tu cuenta y lo hacemos nosotros dentro de 30 días.

Una versión anterior de la app sí sincronizaba movimientos con el
servidor. Esa sincronización se retiró, pero si usaste esas versiones
puede quedar data tuya de esa época: se borra por completo cuando
borras tu cuenta, y la app ya no sube ninguna fila nueva.

## Exportar tu data

Si exportas tu data a un archivo, tú decides qué hacer con él —
guardarlo, mandarlo o borrarlo. Nosotros no recibimos copia.

## Respaldo en la nube (desactivado en la versión publicada)

La app trae un respaldo automático a la nube, pero viene **apagado en
la versión publicada**: hoy no se sube ningún dato financiero tuyo a
ningún servidor.

Cuando se active: con sesión iniciada, la app subirá a Supabase Storage
la exportación JSON completa y versionada de tu data local, un archivo
por copia, y las copias viejas se irán borrando solas. Cada cuenta solo
puede leer sus propios archivos; sin sesión no hay respaldo.

El respaldo es del celular, no de la cuenta: si entras con otra cuenta
en el mismo celular, la data que ya estaba ahí se queda, y la siguiente
copia la subirá al espacio de la nueva cuenta —incluidos los
movimientos que registraste con la cuenta anterior. La app te lo avisa
en pantalla antes de la primera subida a un destino nuevo.

Esta política se actualiza antes de que el respaldo se active.

## No usamos analytics

No usamos cookies ni analytics. No sabemos qué pantallas abres ni
cuántos movimientos registras.

## Crashlytics (solo en la versión publicada)

La versión publicada en Play Store usa Firebase Crashlytics para
reportar errores. Esto envía a Google:

- el modelo de tu celular y la versión de Android;
- el stack trace de los errores, los que tumban la app y los que la app
  alcanza a capturar;
- mensajes de diagnóstico sobre su funcionamiento: qué evento disparó
  un respaldo, el nombre del archivo de respaldo que no se pudo
  limpiar, y en qué paso falló un borrado de cuenta.

Esos mensajes no llevan montos, ni descripciones, ni ningún dato de tus
movimientos. **Nunca enviamos tu data financiera.**

Si quieres una versión sin Crashlytics, puedes compilar la app desde
el código fuente con el flavor `dev`.

## Si reinstalas

Si reinstalas la app o cambias de celular sin exportar primero, la data
se pierde — hoy pasa igual con cuenta y sin cuenta, porque nada de tu
data financiera está en nuestros servidores. Para recuperarla necesitas
un archivo JSON que hayas exportado tú: Perfil → "Importar respaldo".
Iniciar sesión no restaura nada.

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
