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
internal fun ImportBackupDialog(warning: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    EmmDialog(
        title = "¿Reemplazar todo con el respaldo?",
        confirmLabel = "Reemplazar",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
        content = {
            Text(
                text = warning,
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
        ImportBackupDialog(
            warning = ProfileUiState(session = SessionUiState.SignedIn(email = null)).importWarning,
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview
@Composable
private fun ImportBackupDialogSignedOutPreview() {
    EmmTheme {
        ImportBackupDialog(
            warning = ProfileUiState(session = SessionUiState.SignedOut).importWarning,
            onConfirm = {},
            onDismiss = {},
        )
    }
}
