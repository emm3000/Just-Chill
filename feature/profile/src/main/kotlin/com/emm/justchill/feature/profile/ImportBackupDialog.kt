package com.emm.justchill.feature.profile

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun ImportBackupDialog(isSignedIn: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    EmmDialog(
        title = "¿Reemplazar todo con el respaldo?",
        confirmLabel = "Reemplazar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
        content = {
            Text(
                text = if (isSignedIn) {
                    "Tus movimientos, categorías, cuentas y recurrentes quedan tal cual el archivo. " +
                        "Lo que no esté ahí se borra, y como tienes sesión iniciada también se " +
                        "borra en tus otros dispositivos. No se puede deshacer."
                } else {
                    "Tus movimientos, categorías, cuentas y recurrentes quedan tal cual el archivo. " +
                        "Lo que no esté ahí se borra. No se puede deshacer."
                },
                style = LocalEmmType.current.bodyM,
                color = LocalEmmColors.current.textSecondary,
            )
        },
    )
}

@Preview
@Composable
private fun ImportBackupDialogSignedInPreview() {
    EmmTheme {
        ImportBackupDialog(isSignedIn = true, onConfirm = {}, onDismiss = {})
    }
}

@Preview
@Composable
private fun ImportBackupDialogSignedOutPreview() {
    EmmTheme {
        ImportBackupDialog(isSignedIn = false, onConfirm = {}, onDismiss = {})
    }
}
