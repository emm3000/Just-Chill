package com.emm.justchill.hh.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.CommitHash
import com.emm.justchill.core.backup.BackupDisclosureSignal
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmSnackbarHost
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.accountEntries
import com.emm.justchill.hh.auth.authEntries
import com.emm.justchill.hh.category.categoryEntries
import com.emm.justchill.hh.loan.loanEntries
import com.emm.justchill.hh.onboarding.onboardingEntries
import com.emm.justchill.hh.profile.profileEntries
import com.emm.justchill.hh.recurring.recurringEntries
import com.emm.justchill.hh.report.reportEntries
import com.emm.justchill.hh.seetransactions.seeTransactionsEntries
import com.emm.justchill.hh.transaction.SelectableCategory
import com.emm.justchill.hh.transaction.transactionEntries
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun AppNavHost(modifier: Modifier = Modifier, shortcut: ShortcutIntent = ShortcutIntent(), shortcutRequestId: Int = 0) {
    EmmTheme {
        val colors = LocalEmmColors.current
        val appPrefs: AppPreferences = koinInject()
        val appVersion: String = koinInject(named("appVersion"))
        val commitHash: String = koinInject<CommitHash>().value
        val backupDisclosure: BackupDisclosureSignal = koinInject()
        val disclosurePending: Boolean by backupDisclosure.isPending.collectAsStateWithLifecycle(false)

        val startRoute: NavKey = remember {
            if (appPrefs.firstLaunchSeen) startTab else ManifestoRoute()
        }
        val backStack: NavBackStack<NavKey> = rememberNavBackStack(startRoute)
        val hostNav: AppNavigator = rememberAppNavigator(backStack)
        LaunchedEffect(shortcutRequestId) {
            shortcutRouteToPush(shortcut, appPrefs.firstLaunchSeen, backStack.lastOrNull())?.let(hostNav::pushToTop)
        }
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        var pendingImportJson by remember { mutableStateOf<String?>(null) }
        val snackbarHostState = remember { SnackbarHostState() }
        val rootScope = rememberCoroutineScope()
        val showRootMessage: (String) -> Unit = { message ->
            rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
        }
        val platform = rememberPlatformHostActions(
            snackbarHostState = snackbarHostState,
            scope = rootScope,
            onImport = { json -> pendingImportJson = json },
        )

        val currentRoute: NavKey? = backStack.lastOrNull()
        val showBottomBar: Boolean = currentRoute is BottomBarRoute
        val bindings = NavHostBindings(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            showMessage = showRootMessage,
            platform = platform,
        )

        Scaffold(
            modifier = modifier.background(colors.bg),
            snackbarHost = { EmmSnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(tween(300)) { it } + fadeIn(tween(300)),
                    exit = slideOutVertically(tween(250)) { it } + fadeOut(tween(200)),
                ) {
                    HhBottomBar(
                        current = currentRoute as? BottomBarRoute,
                        onTabClick = { tab -> hostNav.switchTab(tab) },
                        onAddClick = { hostNav.push(AddTransactionRoute()) },
                        showProfileBadge = disclosurePending,
                    )
                }
            },
            contentWindowInsets = WindowInsets.safeDrawing,
        ) { padding ->

            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(padding)
                    .consumeWindowInsets(padding),
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    onboardingEntries(bindings, appPrefs)
                    authEntries(bindings)
                    seeTransactionsEntries(bindings)
                    accountEntries(bindings)
                    loanEntries(bindings)
                    categoryEntries(
                        bindings = bindings,
                        onCategoryForTransaction = { created -> pendingCategory = created },
                    )
                    transactionEntries(
                        bindings = bindings,
                        pendingCategory = { pendingCategory },
                        onPendingCategoryConsumed = { pendingCategory = null },
                    )
                    reportEntries(bindings)
                    recurringEntries(bindings)
                    profileEntries(
                        bindings = bindings,
                        appVersion = appVersion,
                        commitHash = commitHash,
                        pendingImportJson = { pendingImportJson },
                        onImportHandled = { pendingImportJson = null },
                    )
                },
            )
        }
    }
}
