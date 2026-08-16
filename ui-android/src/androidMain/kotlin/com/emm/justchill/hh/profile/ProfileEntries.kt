package com.emm.justchill.hh.profile

import android.content.ClipData
import android.os.Build
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.toClipEntry
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.shared.AccountsRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.AuthRoute
import com.emm.justchill.hh.shared.CategoriesListRoute
import com.emm.justchill.hh.shared.ManifestoRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.PrivacyPolicyRoute
import com.emm.justchill.hh.shared.ProfileRoute
import com.emm.justchill.hh.shared.RecurringMovementsRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import com.emm.justchill.hh.shared.toText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.profileEntries(
    bindings: NavHostBindings,
    appVersion: String,
    commitHash: String,
    pendingImportJson: () -> String?,
    onImportHandled: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileEntry(
            bindings = bindings,
            appVersion = appVersion,
            commitHash = commitHash,
            pendingImportJson = pendingImportJson,
            clearPendingImport = onImportHandled,
        )
    }

    entry<PrivacyPolicyRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        PrivacyPolicyScreen(
            onBack = { nav.pop() },
        )
    }
}

@Composable
private fun ProfileEntry(
    bindings: NavHostBindings,
    appVersion: String,
    commitHash: String,
    pendingImportJson: () -> String?,
    clearPendingImport: () -> Unit,
) {
    val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
    val vm: ProfileViewModel = koinViewModel()
    val profileState by vm.state.collectAsStateWithLifecycle()
    val clipboard: Clipboard = LocalClipboard.current
    val clipboardScope: CoroutineScope = rememberCoroutineScope()

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is ProfileEffect.ShowError -> bindings.snackbarHostState.showEmmSnackbar(
                    message = effect.error.toUserMessage(),
                    tone = EmmSnackbarTone.Error,
                )

                is ProfileEffect.ExportReady -> bindings.platform.requestExport(effect.json)

                is ProfileEffect.Notify -> bindings.snackbarHostState.showEmmSnackbar(
                    message = effect.message.toText(),
                    tone = when (effect.message) {
                        ProfileMessage.ExportFailed,
                        ProfileMessage.ImportFailed,
                        ProfileMessage.OperationInProgress,
                        ProfileMessage.BackupFailed,
                        ProfileMessage.BackupNeedsAccount,
                        ProfileMessage.BackupNeedsDisclosure,
                        -> EmmSnackbarTone.Error

                        ProfileMessage.SessionClosed,
                        ProfileMessage.SessionClosedLocallyOnly,
                        ProfileMessage.AccountDeleted,
                        ProfileMessage.ExportDone,
                        is ProfileMessage.ImportDone,
                        ProfileMessage.BackupDone,
                        -> EmmSnackbarTone.Success
                    },
                )
            }
        }
    }

    pendingImportJson()?.let { json ->
        ImportConfirmationDialog(
            onConfirm = {
                vm.onIntent(ProfileIntent.ImportJson(json))
                clearPendingImport()
            },
            onDismiss = clearPendingImport,
        )
    }

    ProfileScreen(
        state = profileState,
        appVersion = appVersion,
        commitHash = commitHash,
        isDebug = bindings.platform.isDebug,
        onCategoriesClick = { nav.push(CategoriesListRoute) },
        onAccountsClick = { nav.push(AccountsRoute) },
        onRecurringClick = { nav.push(RecurringMovementsRoute) },
        onAboutClick = { nav.push(ManifestoRoute(isRevisit = true)) },
        onExportClick = {
            if (bindings.platform.supportsBackup) vm.onIntent(ProfileIntent.ExportRequested)
        },
        onImportClick = { if (bindings.platform.supportsBackup) bindings.platform.requestImport() },
        onPrivacyClick = {
            if (bindings.platform.supportsPrivacyPolicy) nav.push(PrivacyPolicyRoute)
        },
        onSignInClick = { nav.push(AuthRoute) },
        onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
        onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
        onSyncNowClick = { vm.onIntent(ProfileIntent.SyncNow) },
        onBackUpNowClick = { vm.onIntent(ProfileIntent.BackUpNow) },
        onAcknowledgeBackupDestinationClick = {
            vm.onIntent(ProfileIntent.AcknowledgeBackupDestination)
        },
        onCopyCommitHashClick = {
            clipboardScope.launch { copyCommitHash(clipboard, commitHash, bindings) }
        },
    )
}

private const val COMMIT_HASH_CLIP_LABEL = "Commit hash"

private suspend fun copyCommitHash(clipboard: Clipboard, commitHash: String, bindings: NavHostBindings) {
    clipboard.setClipEntry(ClipData.newPlainText(COMMIT_HASH_CLIP_LABEL, commitHash).toClipEntry())
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        bindings.snackbarHostState.showEmmSnackbar("Copiamos el hash del commit")
    }
}

@Composable
private fun ImportConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val dialogColors = LocalEmmColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Reemplazar tu data?") },
        text = { Text("Esto va a borrar todo lo que tengas hoy y poner lo del archivo.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = "Reemplazar todo",
                    color = dialogColors.danger,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
