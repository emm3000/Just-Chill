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

/**
 * Registers the profile entries on the host: [ProfileRoute] and [PrivacyPolicyRoute].
 *
 * @param appVersion the version string shown in the profile footer, injected by the host.
 * @param commitHash the full sha the build came from, injected by the host and shown abbreviated in
 *   that same footer.
 * @param pendingImportJson reads the host's "backup file the user just picked" channel. It is a
 *   lambda, not a value, on purpose: `rememberDecoratedNavEntries` caches the built entries under
 *   `remember(backStack.toList())`, and the Android SAF import callback fires with no back-stack
 *   change at all — an entry closing over a plain value would keep reading `null` and the
 *   confirmation dialog would never open. Reading through the lambda keeps the snapshot read inside
 *   the entry's own composition scope, exactly where it happened before this split.
 * @param onImportHandled clears that channel, whether the user confirmed or dismissed.
 */
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
        // Registered on both platforms; iOS never navigates here (privacy click is inert).
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        PrivacyPolicyScreen(
            onBack = { nav.pop() },
        )
    }
}

// `clearPendingImport` rather than the caller's `onImportHandled`: compose-rules' ParameterNaming
// rejects a past-tense lambda parameter on a composable, and this one is a command ("clear the
// channel"), not an event, so it reads as one — same shape as NavHostBindings.showMessage.
//
// `nav` used to be a sixth parameter, built by the entry{} block above and handed down. It was
// always derived from `bindings.backStack`, which is already here, and detekt caps a function at
// five parameters — so it is built here instead. This composable IS the entry's body, so
// rememberAppNavigator still runs inside the entry scope, which is the only thing that constraint
// is about (see AppNavigator).
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

                // VM finished generating the backup; the platform layer owns the
                // SAF write. No-op on iOS (export is gated off there, so this never
                // fires).
                is ProfileEffect.ExportReady -> bindings.platform.requestExport(effect.json)

                is ProfileEffect.Notify -> bindings.snackbarHostState.showEmmSnackbar(
                    message = effect.message.toText(),
                    tone = when (effect.message) {
                        ProfileMessage.ExportFailed,
                        ProfileMessage.ImportFailed,
                        ProfileMessage.OperationInProgress,
                        ProfileMessage.BackupFailed,
                        ProfileMessage.BackupNeedsAccount,
                        -> EmmSnackbarTone.Error

                        // A chosen branch, not a default: EmmSnackbarTone only has two values today,
                        // so SessionClosedLocallyOnly (the server-side revoke was not reached) is a
                        // deliberate Success here rather than an unclassified fallback — the sign-out
                        // the user asked for did succeed locally. The next message added to
                        // ProfileMessage has to extend this list explicitly; there is no `else`.
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
        // Unlike the export above, this is NOT gated on platform.supportsBackup: that flag is about
        // the SAF file pickers this platform layer owns, and a snapshot cycle needs none of them.
        // Its own gate is SNAPSHOT_BACKUP_ENABLED, which decides whether the row exists at all.
        onBackUpNowClick = { vm.onIntent(ProfileIntent.BackUpNow) },
        onCopyCommitHashClick = {
            clipboardScope.launch { copyCommitHash(clipboard, commitHash, bindings) }
        },
    )
}

/** Label the system attaches to the clip; never shown inside the app. */
private const val COMMIT_HASH_CLIP_LABEL = "Commit hash"

/**
 * Puts the FULL sha on the clipboard and confirms it, on the versions that need confirming.
 *
 * Android 13 (TIRAMISU) added a system-drawn confirmation for every clipboard write, in the same
 * corner of the screen the app's snackbar occupies. Showing both would be two popups saying the
 * same thing, stacked. Below 33 nothing confirms the copy at all, so the app has to — through the
 * root snackbar this screen already reports every other outcome on, not a mechanism of its own.
 */
private suspend fun copyCommitHash(clipboard: Clipboard, commitHash: String, bindings: NavHostBindings) {
    clipboard.setClipEntry(ClipData.newPlainText(COMMIT_HASH_CLIP_LABEL, commitHash).toClipEntry())
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        bindings.snackbarHostState.showEmmSnackbar("Copiamos el hash del commit")
    }
}

/**
 * Confirms the destructive half of a backup import: restoring a file replaces everything on the
 * device, so the user is asked once before the VM is told to do it.
 */
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
