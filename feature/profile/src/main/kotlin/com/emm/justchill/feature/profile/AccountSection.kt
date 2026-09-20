package com.emm.justchill.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emm.justchill.core.ui.atoms.ChevronTrailing

@Composable
internal fun AccountSection(
    session: SessionUiState.SignedIn,
    op: ProfileOp,
    dialog: ProfileDialog,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    if (dialog == ProfileDialog.DeleteAccount) {
        DeleteAccountDialog(
            onConfirm = onDeleteAccountConfirm,
            onDismiss = onDialogDismiss,
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Cuenta")
        ProfileGroup {
            ProfileRowWithTrailing(
                icon = Icons.Outlined.AccountCircle,
                label = session.email ?: "Tu cuenta",
                meta = "",
                metaIsPrimary = false,
                onClick = null,
                trailing = {},
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.Shield,
                label = "Cerrar sesión",
                meta = if (op == ProfileOp.SigningOut) {
                    "Cerrando sesión…"
                } else {
                    "Tus datos siguen en este teléfono"
                },
                metaIsPrimary = false,
                enabled = op == ProfileOp.None || op == ProfileOp.SigningOut,
                onClick = onSignOutClick.takeIf { op == ProfileOp.None },
                trailing = { ChevronTrailing(enabled = op == ProfileOp.None || op == ProfileOp.SigningOut) },
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.Delete,
                label = "Eliminar cuenta",
                meta = if (op == ProfileOp.DeletingAccount) {
                    "Eliminando…"
                } else {
                    "Borra tu cuenta y tus datos en la nube"
                },
                metaIsPrimary = false,
                enabled = op == ProfileOp.None || op == ProfileOp.DeletingAccount,
                onClick = onDeleteAccountClick.takeIf { op == ProfileOp.None },
                trailing = { ChevronTrailing(enabled = op == ProfileOp.None || op == ProfileOp.DeletingAccount) },
            )
        }
    }
}
