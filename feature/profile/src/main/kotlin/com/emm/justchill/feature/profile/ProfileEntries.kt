package com.emm.justchill.feature.profile

import android.content.ClipData
import android.os.Build
import androidx.compose.material3.Text
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
import com.emm.justchill.core.ui.atoms.EmmDialog
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.IconBtnTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.core.ui.error.toUserMessage
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmType
import com.emm.justchill.feature.profile.privacy.PrivacyPolicyRoute
import com.emm.justchill.feature.profile.privacy.PrivacyPolicyScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.profileEntries(
    bindings: NavHostBindings,
    appVersion: String,
    commitHash: String,
    pendingImportJson: () -> String?,
    onImportHandled: () -> Unit,
    onTransactionsClick: (AppNavigator) -> Unit,
    onReportClick: (AppNavigator) -> Unit,
    onAccountsClick: (AppNavigator) -> Unit,
    onCategoriesClick: (AppNavigator) -> Unit,
    onRecurringClick: (AppNavigator) -> Unit,
    onLoansClick: (AppNavigator) -> Unit,
    onAboutClick: (AppNavigator) -> Unit,
    onSignInClick: (AppNavigator) -> Unit,
) {
    entry<ProfileRoute> {
        ProfileEntry(
            bindings = bindings,
            appVersion = appVersion,
            commitHash = commitHash,
            pendingImportJson = pendingImportJson,
            clearPendingImport = onImportHandled,
            onTransactionsClick = onTransactionsClick,
            onReportClick = onReportClick,
            onAccountsClick = onAccountsClick,
            onCategoriesClick = onCategoriesClick,
            onRecurringClick = onRecurringClick,
            onLoansClick = onLoansClick,
            onAboutClick = onAboutClick,
            onSignInClick = onSignInClick,
        )
    }

    entry<PrivacyPolicyRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
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
    onTransactionsClick: (AppNavigator) -> Unit,
    onReportClick: (AppNavigator) -> Unit,
    onAccountsClick: (AppNavigator) -> Unit,
    onCategoriesClick: (AppNavigator) -> Unit,
    onRecurringClick: (AppNavigator) -> Unit,
    onLoansClick: (AppNavigator) -> Unit,
    onAboutClick: (AppNavigator) -> Unit,
    onSignInClick: (AppNavigator) -> Unit,
) {
    val nav: AppNavigator = rememberAppNavigator(bindings.backStack, bindings.startTab)
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

                is ProfileEffect.ExportReady -> bindings.platform.requestExport(effect.json) { saved ->
                    vm.onIntent(ProfileIntent.ExportFinished(saved))
                }

                is ProfileEffect.Notify -> bindings.snackbarHostState.showEmmSnackbar(
                    message = effect.message.toText(),
                    tone = when (effect.message) {
                        ProfileMessage.ExportFailed,
                        ProfileMessage.ImportFailed,
                        ProfileMessage.OperationInProgress,
                        is ProfileMessage.BackupFailed,
                        ProfileMessage.BackupNeedsAccount,
                        ProfileMessage.BackupNeedsDisclosure,
                        ProfileMessage.BackupVerifyFailed,
                        is ProfileMessage.BackupNotVerified,
                        -> EmmSnackbarTone.Error

                        ProfileMessage.SessionClosed,
                        ProfileMessage.SessionClosedLocallyOnly,
                        ProfileMessage.AccountDeleted,
                        ProfileMessage.ExportDone,
                        is ProfileMessage.ImportDone,
                        ProfileMessage.BackupDone,
                        is ProfileMessage.BackupVerified,
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
        onTransactionsClick = { onTransactionsClick(nav) },
        onReportClick = { onReportClick(nav) },
        onAccountsClick = { onAccountsClick(nav) },
        onCategoriesClick = { onCategoriesClick(nav) },
        onRecurringClick = { onRecurringClick(nav) },
        onLoansClick = { onLoansClick(nav) },
        onAboutClick = { onAboutClick(nav) },
        onExportClick = {
            if (bindings.platform.supportsBackup) vm.onIntent(ProfileIntent.ExportRequested)
        },
        onImportClick = { vm.onIntent(ProfileIntent.ImportClicked) },
        onImportConfirm = {
            vm.onIntent(ProfileIntent.DialogDismissed)
            if (bindings.platform.supportsBackup) bindings.platform.requestImport()
        },
        onDialogDismiss = { vm.onIntent(ProfileIntent.DialogDismissed) },
        onPrivacyClick = {
            if (bindings.platform.supportsPrivacyPolicy) nav.push(PrivacyPolicyRoute)
        },
        onSignInClick = { onSignInClick(nav) },
        onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
        onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccountClicked) },
        onDeleteAccountConfirm = { vm.onIntent(ProfileIntent.DeleteAccountConfirmed) },
        onBackUpNowClick = { vm.onIntent(ProfileIntent.BackUpNow) },
        onVerifyBackupClick = { vm.onIntent(ProfileIntent.VerifyBackup) },
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
    EmmDialog(
        title = "¿Reemplazar tu data?",
        confirmLabel = "Reemplazar todo",
        onConfirm = onConfirm,
        dismissLabel = "Cancelar",
        onDismiss = onDismiss,
        confirmTone = IconBtnTone.Danger,
        content = {
            Text(
                text = "Esto va a borrar todo lo que tengas hoy y poner lo del archivo.",
                style = LocalEmmType.current.bodyM,
                color = LocalEmmColors.current.textSecondary,
            )
        },
    )
}
