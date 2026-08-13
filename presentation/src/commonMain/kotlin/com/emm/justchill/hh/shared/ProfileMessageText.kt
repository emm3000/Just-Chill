package com.emm.justchill.hh.shared

import com.emm.justchill.hh.profile.ProfileMessage

// Shared Spanish copy for ProfileViewModel notifications surfaced via the root snackbar. Consumed by
// ui-android/.../hh/profile/ProfileEntries.kt. The strings are UI copy and are preserved verbatim.
fun ProfileMessage.toText(): String = when (this) {
    ProfileMessage.SessionClosed -> "Sesión cerrada. Tus datos siguen en este teléfono."

    ProfileMessage.SessionClosedLocallyOnly ->
        "Sesión cerrada acá; no llegué al servidor, así que tu acceso remoto sigue activo hasta " +
            "que expire. Cerrá sesión con internet para cortarlo. Tus datos siguen en este teléfono."

    ProfileMessage.AccountDeleted -> "Cuenta eliminada. Tus datos siguen en este teléfono."

    ProfileMessage.ExportDone -> "Listo, tu data está guardada."

    ProfileMessage.ExportFailed -> "No pude exportar — capaz no hay espacio en tu celu?"

    is ProfileMessage.ImportDone -> "Listo — $transactions movimientos importados."

    ProfileMessage.ImportFailed -> "No pude importar el archivo — capaz está dañado."

    ProfileMessage.OperationInProgress -> "Espera a que termine la operación en curso."
}
