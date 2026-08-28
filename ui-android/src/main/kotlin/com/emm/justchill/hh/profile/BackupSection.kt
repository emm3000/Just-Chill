package com.emm.justchill.hh.profile

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emm.justchill.core.backup.SNAPSHOT_BACKUP_ENABLED
import com.emm.justchill.core.theme.InterFontFamily
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.theme.LocalEmmRadii
import com.emm.justchill.core.theme.LocalEmmSpacing
import com.emm.justchill.core.ui.atoms.FilledCta
import com.emm.justchill.hh.shared.BACKUP_DESTINATION_DISCLOSURE
import com.emm.justchill.hh.shared.BACKUP_DESTINATION_DISCLOSURE_ACTION
import com.emm.justchill.hh.shared.BACKUP_LOCAL_ONLY_WARNING
import com.emm.justchill.hh.shared.toMetaText

@Composable
internal fun BackupSection(
    state: ProfileUiState,
    onExportClick: () -> Unit,
    onImportClick: () -> Unit,
    onSignInClick: () -> Unit,
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
                meta = if (state.op == ProfileOp.Exporting) "Preparando…" else state.lastExport.toMetaText(),
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None || state.op == ProfileOp.Exporting,
                onClick = onExportClick.takeIf { state.op == ProfileOp.None },
                trailing = {
                    ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.Exporting)
                },
            )
            ProfileRowWithTrailing(
                icon = Icons.Outlined.FileUpload,
                label = "Importar respaldo",
                meta = if (state.op == ProfileOp.Importing) "Importando…" else "Reemplaza todo lo que hay",
                metaIsPrimary = false,
                enabled = state.op == ProfileOp.None || state.op == ProfileOp.Importing,
                onClick = { showImportDialog = true }.takeIf { state.op == ProfileOp.None },
                trailing = {
                    ChevronTrailing(enabled = state.op == ProfileOp.None || state.op == ProfileOp.Importing)
                },
            )
        }
        // The cloud rows and the local-only note are the two branches of ONE read of the kill
        // switch, so the door to an account can never sit above a note promising nothing leaves the
        // phone. Two separate reads would let a future edit negate only one of them.
        if (SNAPSHOT_BACKUP_ENABLED) {
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
            enabled = state.op == ProfileOp.None || state.op == ProfileOp.BackingUp,
            onClick = snapshotActions.onBackUpNow.takeIf { state.op == ProfileOp.None },
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
        enabled = idle || busy,
        onClick = onVerifyClick.takeIf { idle },
        trailing = { ChevronTrailing(enabled = idle || busy) },
    )
}

@Composable
private fun BackupLocalOnlyNote() {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    val radii = LocalEmmRadii.current
    Text(
        text = BACKUP_LOCAL_ONLY_WARNING,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.W400,
        color = colors.textSecondary,
        modifier = Modifier
            .padding(start = spacing.s6, end = spacing.s6, top = spacing.s2)
            .fillMaxWidth()
            .border(width = 1.dp, color = colors.border, shape = radii.rM)
            .padding(horizontal = spacing.s4, vertical = spacing.s3),
    )
}

@Composable
private fun BackupDestinationDisclosure(onAcknowledge: () -> Unit) {
    val colors = LocalEmmColors.current
    val spacing = LocalEmmSpacing.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = spacing.s6, end = spacing.s6, bottom = spacing.s4),
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
