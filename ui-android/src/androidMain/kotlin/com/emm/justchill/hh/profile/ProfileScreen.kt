package com.emm.justchill.hh.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SyncDisabled
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.backup.SNAPSHOT_BACKUP_ENABLED
import com.emm.justchill.core.sync.SYNC_TEMPORARILY_DISABLED
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.theme.LocalEmmType
import com.emm.justchill.core.ui.atoms.Eyebrow
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.hh.shared.BACKUP_DESTINATION_DISCLOSURE
import com.emm.justchill.hh.shared.BACKUP_DESTINATION_DISCLOSURE_ACTION
import com.emm.justchill.hh.shared.SpanishDateFormat
import com.emm.justchill.hh.shared.toMetaText
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

private const val SYNC_PAUSED_META = "Sincronización en pausa"

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    appVersion: String = "",
    commitHash: String = "",
    isDebug: Boolean = false,
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
    onSyncNowClick: () -> Unit = {},
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
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding(),
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
            onSyncNowClick = onSyncNowClick,
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

        if (isDebug) {
            SectionHeader(text = "Debug")
            ProfileGroup {
                ProfileRow(
                    icon = Icons.Outlined.Repeat,
                    label = "Sincronizar ahora",
                    meta = when {
                        SYNC_TEMPORARILY_DISABLED -> "$SYNC_PAUSED_META · este botón no hace nada"
                        state.isSyncing -> "Sincronizando…"
                        else -> "Push + Pull manual"
                    },
                    metaIsPrimary = state.isSyncing,
                    onClick = onSyncNowClick,
                )
            }
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

// The three callbacks that only exist behind SNAPSHOT_BACKUP_ENABLED travel together so they can
// leave together, and so BackupSection stays inside the parameter budget.
private class SnapshotBackupActions(
    val onBackUpNow: () -> Unit,
    val onVerify: () -> Unit,
    val onAcknowledgeDestination: () -> Unit,
)

@Composable
private fun BackupSection(
    state: ProfileUiState,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    snapshotActions: SnapshotBackupActions,
) {
    var showImportDialog by remember { mutableStateOf(false) }

    if (showImportDialog) {
        ImportBackupDialog(
            isSignedIn = state.session is SessionUiState.SignedIn,
            onConfirm = {
                showImportDialog = false
                onImportClick()
            },
            onDismiss = { showImportDialog = false },
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Respaldo")
        ProfileGroup {
            ProfileRowWithTrailing(
                icon = Icons.Outlined.FileDownload,
                label = "Exportar mi data",
                meta = if (state.op == ProfileOp.Exporting) "Preparando…" else "Guardar como archivo",
                metaIsPrimary = true,
                enabled = state.op == ProfileOp.None || state.op == ProfileOp.Exporting,
                onClick = onExportClick.takeIf { state.op == ProfileOp.None },
                trailing = {
                    ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.Exporting)
                },
            )
            HairlineDivider()
            ProfileRowWithTrailing(
                icon = Icons.Outlined.FileUpload,
                label = "Importar respaldo",
                meta = if (state.op == ProfileOp.Importing) "Importando…" else "Reemplaza todo",
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None || state.op == ProfileOp.Importing,
                onClick = { showImportDialog = true }.takeIf { state.op == ProfileOp.None },
                trailing = {
                    ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.Importing)
                },
            )
            if (SNAPSHOT_BACKUP_ENABLED) {
                HairlineDivider()
                ProfileRowWithTrailing(
                    icon = Icons.Outlined.CloudUpload,
                    label = "Respaldar ahora",
                    meta = if (state.op == ProfileOp.BackingUp) "Respaldando…" else "Sube una copia a la nube",
                    metaIsPrimary = true,
                    enabled = state.op == ProfileOp.None || state.op == ProfileOp.BackingUp,
                    onClick = snapshotActions.onBackUpNow.takeIf { state.op == ProfileOp.None },
                    trailing = {
                        ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.BackingUp)
                    },
                )
                HairlineDivider()
                VerifyBackupRow(op = state.op, onVerifyClick = snapshotActions.onVerify)
                HairlineDivider()
                LastBackupRow(row = state.backupRow)
                if (state.backupRow == BackupRowUi.DisclosurePending) {
                    BackupDestinationDisclosure(onAcknowledge = snapshotActions.onAcknowledgeDestination)
                }
            }
        }
    }
}

@Composable
private fun VerifyBackupRow(op: ProfileOp, onVerifyClick: () -> Unit) {
    val busy: Boolean = op == ProfileOp.VerifyingBackup
    val idle: Boolean = op == ProfileOp.None
    ProfileRowWithTrailing(
        icon = Icons.Outlined.CloudSync,
        label = "Verificar respaldo",
        meta = if (busy) "Verificando…" else "Revisa que el último se pueda restaurar",
        metaIsPrimary = true,
        enabled = idle || busy,
        onClick = onVerifyClick.takeIf { idle },
        trailing = { ChevronTrailing(enabled = idle || busy) },
    )
}

@Composable
private fun BackupDestinationDisclosure(onAcknowledge: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s5, end = spacing.s5, bottom = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Text(
            text = BACKUP_DESTINATION_DISCLOSURE,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.W400,
            color = colors.textSecondary,
        )
        FilledCta(label = BACKUP_DESTINATION_DISCLOSURE_ACTION, onClick = onAcknowledge)
    }
}

@Composable
private fun LastBackupRow(row: BackupRowUi) {
    val colors = LocalEmmColors.current
    val metaColor: Color? = when (row.severity()) {
        BackupRowSeverity.Normal -> null
        BackupRowSeverity.Warning -> colors.warning
        BackupRowSeverity.Danger -> colors.danger
    }
    ProfileRowWithTrailing(
        icon = Icons.Outlined.CloudDone,
        label = "Último respaldo",
        meta = row.toMetaText(),
        metaIsPrimary = true,
        metaColor = metaColor,
        onClick = null,
        trailing = {
            if (row is BackupRowUi.BackingUp) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = colors.textTertiary,
                )
            }
        },
    )
}

// CyclomaticComplexMethod: every extra branch here comes from SYNC_TEMPORARILY_DISABLED and
// disappears with it — drop this suppression with the kill switch instead of restructuring around it.
@Suppress("CyclomaticComplexMethod")
@Composable
private fun AccountSection(
    state: ProfileUiState,
    onSignInClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit,
    onSyncNowClick: () -> Unit,
) {
    val colors = LocalEmmColors.current
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
                        meta = if (SYNC_TEMPORARILY_DISABLED) {
                            SYNC_PAUSED_META
                        } else {
                            "Sincroniza tus datos entre dispositivos"
                        },
                        metaIsPrimary = false,
                        onClick = onSignInClick,
                    )
                }

                is SessionUiState.SignedIn -> {
                    ProfileRowWithTrailing(
                        icon = Icons.Outlined.AccountCircle,
                        label = session.email ?: "Tu cuenta",
                        meta = if (SYNC_TEMPORARILY_DISABLED) {
                            SYNC_PAUSED_META
                        } else {
                            when (val row = state.syncRow) {
                                SyncRowUi.Syncing -> "Sincronizando…"
                                SyncRowUi.Failed -> "No se pudo sincronizar"
                                is SyncRowUi.Idle -> syncStatusLabel(row.lastSyncedAtMillis)
                            }
                        },
                        metaIsPrimary = true,
                        metaColor = when {
                            SYNC_TEMPORARILY_DISABLED -> colors.warning
                            state.syncRow is SyncRowUi.Failed -> colors.danger
                            else -> null
                        },
                        onClick = null,
                        trailing = {
                            if (SYNC_TEMPORARILY_DISABLED) {
                                Icon(
                                    imageVector = Icons.Outlined.SyncDisabled,
                                    contentDescription = null,
                                    tint = colors.warning,
                                    modifier = Modifier.size(16.dp),
                                )
                            } else {
                                when (state.syncRow) {
                                    SyncRowUi.Syncing -> CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = colors.textTertiary,
                                    )

                                    SyncRowUi.Failed -> RetryPill(onClick = onSyncNowClick)

                                    is SyncRowUi.Idle -> Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = null,
                                        tint = colors.textTertiary,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                        },
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

private fun syncStatusLabel(lastSyncedAtMillis: Long?): String = if (lastSyncedAtMillis != null) {
    val dateTime = Instant.fromEpochMilliseconds(lastSyncedAtMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    "Última sincronización: ${SpanishDateFormat.dayShortMonthTime(dateTime)}"
} else {
    "Sincronización activa"
}

@Composable
private fun SectionHeader(text: String) {
    val spacing = LocalEmmSpacing.current
    Eyebrow(
        text = text,
        modifier = Modifier.padding(
            start = spacing.s5,
            end = spacing.s5,
            top = spacing.s5,
            bottom = spacing.s2,
        ),
    )
}

@Composable
private fun ProfileGroup(content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        content()
    }
}

@Composable
private fun HairlineDivider() {
    val colors = LocalEmmColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.border),
    )
}

@Composable
private fun ProfileRow(icon: ImageVector, label: String, meta: String, metaIsPrimary: Boolean, onClick: () -> Unit) {
    ProfileRowWithTrailing(
        icon = icon,
        label = label,
        meta = meta,
        metaIsPrimary = metaIsPrimary,
        onClick = onClick,
        trailing = { ChevronTrailing(enabled = true) },
    )
}

@Composable
private fun ChevronTrailing(enabled: Boolean) {
    val colors = LocalEmmColors.current
    Icon(
        imageVector = Icons.Outlined.ChevronRight,
        contentDescription = null,
        tint = if (enabled) colors.textTertiary else colors.textDisabled,
        modifier = Modifier.size(16.dp),
    )
}

@Composable
private fun ProfileRowWithTrailing(
    icon: ImageVector,
    label: String,
    meta: String,
    metaIsPrimary: Boolean,
    onClick: (() -> Unit)?,
    trailing: @Composable () -> Unit,
    metaColor: Color? = null,
    enabled: Boolean = true,
) {
    val colors = LocalEmmColors.current
    val type = LocalEmmType.current
    val spacing = LocalEmmSpacing.current

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val bg: Color = if (onClick != null && isPressed) colors.surface1 else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = spacing.s5, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        IconTileSmall(icon = icon, tint = if (enabled) colors.textSecondary else colors.textDisabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = type.bodyL.copy(fontWeight = FontWeight.W500),
                color = if (enabled) colors.textPrimary else colors.textDisabled,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = meta,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.W400,
                color = if (enabled) {
                    metaColor ?: if (metaIsPrimary) colors.textSecondary else colors.textTertiary
                } else {
                    colors.textDisabled
                },
            )
        }
        trailing()
    }
}

@Composable
private fun IconTileSmall(icon: ImageVector, tint: Color = LocalEmmColors.current.textSecondary) {
    val colors = LocalEmmColors.current
    val radii = LocalEmmRadii.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(radii.rXS)
            .background(colors.surface2),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(17.dp),
        )
    }
}

@Composable
private fun VersionFooter(appVersion: String, commitHash: String, onCopyClick: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Versión $appVersion · alpha",
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
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
                fontSize = 12.sp,
                fontFamily = InterFontFamily,
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
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    Row(
        modifier = Modifier
            .clip(radii.rXS)
            .clickable(onClick = onCopyClick)
            .semantics { role = Role.Button }
            .heightIn(min = 48.dp)
            .padding(horizontal = spacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.s2),
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontFamily = InterFontFamily,
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
