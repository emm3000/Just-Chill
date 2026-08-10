package com.emm.justchill.hh.shared

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmSnackbarHost
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.accountEntries
import com.emm.justchill.hh.auth.authEntries
import com.emm.justchill.hh.category.categoryEntries
import com.emm.justchill.hh.home.homeEntries
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

// Single Compose Multiplatform nav host for both Android and iOS. Merges the two former hosts
// (androidApp Hh.kt + iosMain IosApp.kt) into one commonMain composable. The route keys, bottom bar,
// sync-event handling and ProfileMessage copy already live in commonMain; this slice unifies the
// NavDisplay + entryProvider body too. The five capabilities that genuinely differ per platform
// (backup export/import, share, open-email, privacy click + the start tab) sit behind expect/actual
// (PlatformHostActions + startTab) — Android wires real intents, iOS no-ops, exactly as before.
//
// Both platforms now run on the JetBrains Compose Multiplatform navigation3-UI port (NavDisplay) over
// Google's multiplatform navigation3-runtime (NavKey/NavBackStack). EmmTheme is applied here so each
// entry point only calls AppNavHost().
//
// The fifteen entries themselves are NOT here: each feature owns its own
// `EntryProviderScope<NavKey>.xxxEntries(...)` next to the screens it wires (hh/<feature>/
// <Feature>Entries.kt). What is left is the host proper — theme, DI lookups, the first-launch gate,
// the back stack, the two result channels, the snackbar host, sync events, the bottom bar and
// NavDisplay. Each entries function builds its own in-scene AppNavigator inside its entry{} body;
// see AppNavigator for why that call cannot be hoisted up to here.

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    EmmTheme {
        val colors = LocalEmmColors.current
        val appPrefs: AppPreferences = koinInject()
        // Inject via the SyncController port so the SAME instance the orchestrator emits on drives the
        // sync-event snackbars. Android binds SyncOrchestrator to SyncController; iOS binds it too.
        val syncController: SyncController = koinInject()
        val appVersion: String = koinInject(named("appVersion"))

        // First-launch Manifesto gate: show the manifesto once, then land on startTab on every
        // subsequent launch.
        val startRoute: NavKey = remember {
            if (appPrefs.firstLaunchSeen) startTab else ManifestoRoute()
        }
        // 2-arg rememberNavBackStack with an explicit SavedStateConfiguration on BOTH platforms (one
        // host, one call). iOS needs it (no K/N reflection serializer discovery); Android tolerates it.
        val backStack: NavBackStack<NavKey> =
            rememberNavBackStack(navSavedStateConfiguration, startRoute)
        // Host-level navigator for the two call sites that compose OUTSIDE NavDisplay: the bottom bar
        // and SyncEventsHandler. Only its duplicate-key guard is live here — see AppNavigator. Every
        // entry below builds its own, in-scene, where the transition guard works too.
        val hostNav: AppNavigator = rememberAppNavigator(backStack)
        var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
        // Holds the JSON contents of an imported backup file until the user confirms the destructive
        // replace. Hoisted to the host so the Android SAF import launcher (created in
        // rememberPlatformHostActions, above NavDisplay) can feed it back; the Profile entry renders
        // the confirmation dialog. Never set on iOS (requestImport no-ops there).
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

        SyncEventsHandler(
            syncController = syncController,
            snackbarHostState = snackbarHostState,
            onNavigateToSignIn = { hostNav.push(AuthRoute) },
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
                        onAddClick = { hostNav.push(AddTransactionRoute) },
                    )
                }
            },
            contentWindowInsets = WindowInsets(0),
        ) { padding ->

            NavDisplay(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.bg)
                    .padding(padding),
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    onboardingEntries(bindings, appPrefs)
                    authEntries(bindings)
                    homeEntries(bindings)
                    seeTransactionsEntries(bindings)
                    accountEntries(bindings)
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
                        pendingImportJson = { pendingImportJson },
                        onImportHandled = { pendingImportJson = null },
                    )
                },
            )
        }
    }
}
