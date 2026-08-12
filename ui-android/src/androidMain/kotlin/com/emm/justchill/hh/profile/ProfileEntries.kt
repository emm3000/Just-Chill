package com.emm.justchill.hh.profile

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import org.koin.compose.viewmodel.koinViewModel

/**
 * Registers the profile entries on the host: [ProfileRoute] and [PrivacyPolicyRoute].
 *
 * @param appVersion the version string shown in the profile footer, injected by the host.
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
    pendingImportJson: () -> String?,
    onImportHandled: () -> Unit,
) {
    entry<ProfileRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        ProfileEntry(
            nav = nav,
            bindings = bindings,
            appVersion = appVersion,
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
@Composable
private fun ProfileEntry(
    nav: AppNavigator,
    bindings: NavHostBindings,
    appVersion: String,
    pendingImportJson: () -> String?,
    clearPendingImport: () -> Unit,
) {
    val vm: ProfileViewModel = koinViewModel()
    val profileState by vm.state.collectAsStateWithLifecycle()

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
                        -> EmmSnackbarTone.Error

                        else -> EmmSnackbarTone.Success
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
    )
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
