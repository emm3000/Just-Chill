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
    state: ProfileUiState,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onDeleteAccountConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
) {
    if (state.dialog == ProfileDialog.DeleteAccount) {
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
                label = state.accountLabel,
                meta = "",
                metaIsPrimary = false,
                onClick = null,
                trailing = {},
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.Shield,
                label = "Cerrar sesión",
                meta = state.signOutMeta,
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None,
                busy = state.op == ProfileOp.SigningOut,
                onClick = onSignOutClick,
                trailing = {
                    ChevronTrailing(
                        enabled = state.op == ProfileOp.None || state.op == ProfileOp.SigningOut,
                    )
                },
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.Delete,
                label = "Eliminar cuenta",
                meta = state.deleteAccountMeta,
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None,
                busy = state.op == ProfileOp.DeletingAccount,
                onClick = onDeleteAccountClick,
                trailing = {
                    ChevronTrailing(
                        enabled = state.op == ProfileOp.None || state.op == ProfileOp.DeletingAccount,
                    )
                },
            )
        }
    }
}
