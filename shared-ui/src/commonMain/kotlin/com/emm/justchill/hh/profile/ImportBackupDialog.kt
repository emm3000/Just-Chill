package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmType

/**
 * Confirmation for restoring a backup — the most destructive action in the app.
 *
 * It used to have none at all, while "Eliminar cuenta" did. Restoring replaces every movement,
 * category and account with what the file carries, and while signed in the replacement is pushed
 * to the other devices too, so [isSignedIn] decides how far the warning has to reach.
 */
@Composable
internal fun ImportBackupDialog(isSignedIn: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalEmmColors.current
    val typography = LocalEmmType.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(colors.surface2)
                .padding(20.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.negMuted)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp)),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileUpload,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "¿Reemplazar todo con el respaldo?",
                style = typography.headlineM,
                color = colors.textPrimary,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isSignedIn) {
                    "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                        "Lo que no esté ahí se borra, y como tenés sesión iniciada también se " +
                        "borra en tus otros dispositivos. No se puede deshacer."
                } else {
                    "Tus movimientos, categorías y cuentas quedan tal cual el archivo. " +
                        "Lo que no esté ahí se borra. No se puede deshacer."
                },
                style = typography.bodyM,
                color = colors.textSecondary,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                DialogActionButton(
                    label = "Cancelar",
                    style = colors.neutralDialogAction(),
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                DialogActionButton(
                    label = "Reemplazar",
                    style = colors.destructiveDialogAction(),
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
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
