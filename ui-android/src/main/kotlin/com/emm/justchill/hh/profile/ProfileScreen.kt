package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    appVersion: String = "",
    commitHash: String = "",
    onCategoriesClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onCopyCommitHashClick: () -> Unit = {},
    onBackUpNowClick: () -> Unit = {},
    onVerifyBackupClick: () -> Unit = {},
    onAcknowledgeBackupDestinationClick: () -> Unit = {},
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = "Perfil",
            style = type.headlineL,
            color = colors.textPrimary,
            modifier = Modifier.padding(
                start = spacing.s5,
                end = spacing.s5,
                top = spacing.s6,
                bottom = spacing.s4,
            ),
        )

        AccountSection(
            state = state,
            onSignInClick = onSignInClick,
            onSignOutClick = onSignOutClick,
            onDeleteAccountClick = onDeleteAccountClick,
        )

        SectionHeader(text = "Gestionar")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Category,
                label = "Categorías",
                meta = "${state.categoryCount} totales",
                metaIsPrimary = true,
                onClick = onCategoriesClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.AccountBalanceWallet,
                label = "Cuentas",
                meta = "${state.accountCount} activas",
                metaIsPrimary = true,
                onClick = onAccountsClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.Repeat,
                label = "Movimientos recurrentes",
                meta = "Pagos y cobros fijos",
                metaIsPrimary = false,
                onClick = onRecurringClick,
            )
        }

        BackupSection(
            state = state,
            onExportClick = onExportClick,
            onImportClick = onImportClick,
            snapshotActions = SnapshotBackupActions(
                onBackUpNow = onBackUpNowClick,
                onVerify = onVerifyBackupClick,
                onAcknowledgeDestination = onAcknowledgeBackupDestinationClick,
            ),
        )

        SectionHeader(text = "App")
        ProfileGroup {
            ProfileRow(
                icon = Icons.Outlined.Info,
                label = "Acerca de JustChill",
                meta = "El manifiesto",
                metaIsPrimary = false,
                onClick = onAboutClick,
            )
            HairlineDivider()
            ProfileRow(
                icon = Icons.Outlined.Shield,
                label = "Privacidad",
                meta = "100% local",
                metaIsPrimary = false,
                onClick = onPrivacyClick,
            )
        }

        Spacer(Modifier.height(spacing.s6))
        VersionFooter(
            appVersion = appVersion,
            commitHash = commitHash,
            onCopyClick = onCopyCommitHashClick,
        )
        Spacer(Modifier.height(spacing.s4))
    }
}

@Composable
private fun AccountSection(
    state: ProfileUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
) {
    var showDeleteAccountDialog by remember { mutableStateOf(false) }

    if (showDeleteAccountDialog) {
        DeleteAccountDialog(
            onConfirm = {
                showDeleteAccountDialog = false
                onDeleteAccountClick()
            },
            onDismiss = { showDeleteAccountDialog = false },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Cuenta")
        ProfileGroup {
            when (val session = state.session) {
                SessionUiState.Initializing,
                SessionUiState.SignedOut,
                -> {
                    ProfileRow(
                        icon = Icons.Outlined.Shield,
                        label = "Iniciar sesión",
                        meta = "Respalda tus datos en la nube",
                        metaIsPrimary = false,
                        onClick = onSignInClick,
                    )
                }

                is SessionUiState.SignedIn -> {
                    ProfileRowWithTrailing(
                        icon = Icons.Outlined.AccountCircle,
                        label = session.email ?: "Tu cuenta",
                        meta = "",
                        metaIsPrimary = false,
                        onClick = null,
                        trailing = {},
                    )
                    HairlineDivider()
                    ProfileRowWithTrailing(
                        icon = Icons.Outlined.Shield,
                        label = "Cerrar sesión",
                        meta = if (state.op == ProfileOp.SigningOut) {
                            "Cerrando sesión…"
                        } else {
                            "Tus datos siguen en este teléfono"
                        },
                        metaIsPrimary = false,
                        enabled = state.op == ProfileOp.None || state.op == ProfileOp.SigningOut,
                        onClick = onSignOutClick.takeIf { state.op == ProfileOp.None },
                        trailing = {
                            ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.SigningOut)
                        },
                    )
                    HairlineDivider()
                    ProfileRowWithTrailing(
                        icon = Icons.Outlined.Delete,
                        label = "Eliminar cuenta",
                        meta = if (state.op == ProfileOp.DeletingAccount) {
                            "Eliminando…"
                        } else {
                            "Borra tu cuenta y tus datos en la nube"
                        },
                        metaIsPrimary = false,
                        enabled = state.op == ProfileOp.None || state.op == ProfileOp.DeletingAccount,
                        onClick = { showDeleteAccountDialog = true }.takeIf { state.op == ProfileOp.None },
                        trailing = {
                            ChevronTrailing(
                                enabled = state.op == ProfileOp.None || state.op == ProfileOp.DeletingAccount,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun VersionFooter(appVersion: String, commitHash: String, onCopyClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Versión $appVersion · alpha",
            style = type.caption,
            color = colors.textTertiary,
            textAlign = TextAlign.Center,
        )
        when (val commit = commitHashUi(commitHash)) {
            is CommitHashUi.Available -> CopyableCommitRow(
                label = commit.label,
                onCopyClick = onCopyClick,
            )

            CommitHashUi.Unavailable -> Text(
                text = commit.label,
                style = type.caption,
                color = colors.textTertiary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = spacing.s4, vertical = spacing.s2),
            )
        }
    }
}

@Composable
private fun CopyableCommitRow(label: String, onCopyClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .clip(radii.rXS)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onCopyClick,
            )
            .semantics { role = Role.Button }
            .heightIn(min = 48.dp)
            .padding(horizontal = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = label,
            style = type.caption,
            color = colors.textTertiary,
        )
        Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copiar el hash completo del commit",
            tint = colors.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    EmmTheme {
        ProfileScreen(
            state = ProfileUiState(categoryCount = 12, accountCount = 5),
            appVersion = "1.0.0",
            commitHash = "4e47828d1f2a3b4c5d6e7f8091a2b3c4d5e6f708",
        )
    }
}
