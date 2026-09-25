package com.emm.justchill.feature.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.emm.justchill.core.ui.atoms.ChevronTrailing
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmRadii
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmType
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmRadii
import com.emm.justchill.core.ui.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.theme.LocalEmmType

@Composable
internal fun BackupSection(
    state: ProfileUiState,
    exportActions: ExportActions,
    onImportClick: () -> Unit,
    onImportConfirm: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSignInClick: () -> Unit,
    snapshotActions: SnapshotBackupActions,
) {
    if (state.dialog == ProfileDialog.Import) {
        ImportBackupDialog(
            isSignedIn = state.session is SessionUiState.SignedIn,
            onConfirm = onImportConfirm,
            onDismiss = onDialogDismiss,
        )
    }

    if (state.dialog == ProfileDialog.Export) {
        ExportSheet(op = state.op, actions = exportActions, onDismiss = onDialogDismiss)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        SectionHeader(text = "Respaldo")
        ProfileGroup {
            ProfileRowWithTrailing(
                icon = Icons.Outlined.FileDownload,
                label = "Exportar mi data",
                meta = if (state.op == ProfileOp.Exporting) "Preparando…" else state.lastExport.toMetaText(),
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None,
                busy = state.op == ProfileOp.Exporting,
                onClick = exportActions.onOpen,
                trailing = {},
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.FileUpload,
                label = "Importar respaldo",
                meta = if (state.op == ProfileOp.Importing) "Importando…" else "Reemplaza todo lo que hay",
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None,
                busy = state.op == ProfileOp.Importing,
                onClick = onImportClick,
                trailing = {
                    ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.Importing)
                },
            )
        }
        if (state.isCloudBackupAvailable) {
            CloudBackupRows(state = state, onSignIn = onSignInClick, snapshotActions = snapshotActions)
        } else {
            BackupLocalOnlyNote()
        }
    }
}

@Composable
private fun CloudBackupRows(state: ProfileUiState, onSignIn: () -> Unit, snapshotActions: SnapshotBackupActions) {
    ProfileGroup {
        if (state.session !is SessionUiState.SignedIn) {
            ProfileRow(
                icon = Icons.Outlined.Shield,
                label = "Iniciar sesión",
                meta = "Respalda tus datos en la nube",
                metaIsPrimary = false,
                onClick = onSignIn,
            )
        }
        ProfileRowWithTrailing(
            icon = Icons.Outlined.CloudUpload,
            label = "Respaldar ahora",
            meta = if (state.op == ProfileOp.BackingUp) "Respaldando…" else "Sube una copia a la nube",
            metaIsPrimary = true,
            enabled = state.op == ProfileOp.None,
            busy = state.op == ProfileOp.BackingUp,
            onClick = snapshotActions.onBackUpNow,
            trailing = {
                ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.BackingUp)
            },
        )
        VerifyBackupRow(op = state.op, onVerifyClick = snapshotActions.onVerify)
        LastBackupRow(row = state.backupRow)
        if (state.backupRow == BackupRowUi.DisclosurePending) {
            BackupDestinationDisclosure(onAcknowledge = snapshotActions.onAcknowledgeDestination)
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
        enabled = idle,
        busy = busy,
        onClick = onVerifyClick,
        trailing = { ChevronTrailing(enabled = idle || busy) },
    )
}

@Composable
private fun BackupLocalOnlyNote() {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    val radii: EmmRadii = LocalEmmRadii.current
    Text(
        text = BACKUP_LOCAL_ONLY_WARNING,
        style = type.bodyM,
        color = colors.textSecondary,
        modifier = Modifier
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s2)
            .fillMaxWidth()
            .border(width = spacing.hairline, color = colors.border, shape = radii.rM)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
    )
}

@Composable
private fun BackupDestinationDisclosure(onAcknowledge: () -> Unit) {
    val colors: EmmColors = LocalEmmColors.current
    val type: EmmType = LocalEmmType.current
    val spacing: EmmSpacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, bottom = spacing.s4),
        verticalArrangement = Arrangement.spacedBy(spacing.s3),
    ) {
        Text(
            text = BACKUP_DESTINATION_DISCLOSURE,
            style = type.bodyM,
            color = colors.textSecondary,
        )
        FilledCta(label = BACKUP_DESTINATION_DISCLOSURE_ACTION, onClick = onAcknowledge)
    }
}

@Composable
private fun LastBackupRow(row: BackupRowUi) {
    val colors: EmmColors = LocalEmmColors.current
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
        trailing = {},
    )
}
