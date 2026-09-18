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
internal fun DeleteAccountDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    EmmDialog(
        title = "¿Eliminar tu cuenta?",
        confirmLabel = "Eliminar cuenta",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
        content = {
            Text(
                text = "Se borra tu cuenta y todos tus datos en la nube. " +
                    "Tu plata sigue acá, en este teléfono — eso no se toca.",
                style = LocalEmmType.current.bodyM,
                color = LocalEmmColors.current.textSecondary,
            )
        },
    )
}

@Preview
@Composable
private fun DeleteAccountDialogPreview() {
    EmmTheme {
        DeleteAccountDialog(
            onConfirm = {},
            onDismiss = {},
        )
    }
}
