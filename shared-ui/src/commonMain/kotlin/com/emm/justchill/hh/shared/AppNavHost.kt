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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.theme.EmmTheme
import com.emm.justchill.core.theme.LocalEmmColors
import com.emm.justchill.core.ui.atoms.EmmSnackbarHost
import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.core.ui.atoms.showEmmSnackbar
import com.emm.justchill.hh.account.AccountsEffect
import com.emm.justchill.hh.account.AccountsScreen
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountScreen
import com.emm.justchill.hh.auth.AuthScreen
import com.emm.justchill.hh.category.AddCategoryScreen
import com.emm.justchill.hh.category.AppIconCatalog
import com.emm.justchill.hh.category.CategoriesEffect
import com.emm.justchill.hh.category.CategoriesScreen
import com.emm.justchill.hh.category.CategoriesViewModel
import com.emm.justchill.hh.category.findById
import com.emm.justchill.hh.home.HomeEffect
import com.emm.justchill.hh.home.HomeScreen
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.onboarding.ManifestoScreen
import com.emm.justchill.hh.profile.PrivacyPolicyScreen
import com.emm.justchill.hh.profile.ProfileEffect
import com.emm.justchill.hh.profile.ProfileIntent
import com.emm.justchill.hh.profile.ProfileMessage
import com.emm.justchill.hh.profile.ProfileScreen
import com.emm.justchill.hh.profile.ProfileViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementScreen
import com.emm.justchill.hh.recurring.RecurringMovementsEffect
import com.emm.justchill.hh.recurring.RecurringMovementsScreen
import com.emm.justchill.hh.recurring.RecurringMovementsViewModel
import com.emm.justchill.hh.report.ReportScreen
import com.emm.justchill.hh.seetransactions.SeeTransactionsScreen
import com.emm.justchill.hh.transaction.AddTransactionIntent
import com.emm.justchill.hh.transaction.AddTransactionScreen
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransaction
import com.emm.justchill.hh.transaction.SelectableCategory
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
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

@Suppress("CyclomaticComplexMethod", "LongMethod")
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
            // Guard: only push AuthRoute if it is not anywhere in the back stack.
            onNavigateToSignIn = { if (backStack.none { it is AuthRoute }) backStack.add(AuthRoute) },
        )

        val currentRoute: NavKey? = backStack.lastOrNull()
        val showBottomBar: Boolean = currentRoute is BottomBarRoute

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
                        onTabClick = { tab -> backStack.switchTab(tab, startTab) },
                        onAddClick = { backStack.add(AddTransactionRoute) },
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
                onBack = { backStack.removeLastOrNull() },
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<ManifestoRoute> { key ->
                        ManifestoScreen(
                            isRevisit = key.isRevisit,
                            onStart = {
                                if (key.isRevisit) {
                                    backStack.removeLastOrNull()
                                } else {
                                    appPrefs.firstLaunchSeen = true
                                    backStack.replaceAll(startTab)
                                }
                            },
                        )
                    }

                    entry<PrivacyPolicyRoute> {
                        // Registered on both platforms; iOS never navigates here (privacy click is inert).
                        PrivacyPolicyScreen(
                            onBack = { backStack.removeLastOrNull() },
                        )
                    }

                    entry<AuthRoute> {
                        AuthScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onOpenEmailApp = platform.onOpenEmailApp,
                            showGoogleSignIn = platform.showGoogleSignIn,
                        )
                    }

                    entry<HomeRoute> {
                        HomeEntry(
                            navigateToAll = dropUnlessResumed {
                                backStack.switchTab(SeeTransactionRoute, startTab)
                            },
                            navigateToAdd = { backStack.add(AddTransactionRoute) },
                            navigateToEdit = { id -> backStack.add(EditTransactionRoute(id)) },
                            navigateToReport = { backStack.add(ReportRoute) },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<SeeTransactionRoute> {
                        SeeTransactionsScreen(
                            onEditTransaction = { id ->
                                backStack.add(EditTransactionRoute(id))
                            },
                        )
                    }

                    entry<AccountsRoute> {
                        val vm: AccountsViewModel = koinViewModel()
                        val accountsState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is AccountsEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }

                        AccountsScreen(
                            state = accountsState,
                            onIntent = vm::onIntent,
                            addCategory = { backStack.add(CategoryRoute()) },
                            addAccount = { backStack.add(AddAccountRoute) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<CategoriesListRoute> {
                        val vm: CategoriesViewModel = koinViewModel()
                        val categoriesState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is CategoriesEffect.ShowMessage -> showRootMessage(effect.text)
                                }
                            }
                        }

                        CategoriesScreen(
                            state = categoriesState,
                            onIntent = vm::onIntent,
                            onAddCategory = { backStack.add(CategoryRoute()) },
                            onBack = { backStack.removeLastOrNull() },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    entry<ProfileRoute> {
                        val vm: ProfileViewModel = koinViewModel()
                        val profileState by vm.state.collectAsStateWithLifecycle()

                        LaunchedEffect(vm) {
                            vm.effect.collect { effect ->
                                when (effect) {
                                    is ProfileEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                                        message = effect.error.toUserMessage(),
                                        tone = EmmSnackbarTone.Error,
                                    )

                                    // VM finished generating the backup; the platform layer owns the
                                    // SAF write. No-op on iOS (export is gated off there, so this never
                                    // fires).
                                    is ProfileEffect.ExportReady -> platform.requestExport(effect.json)

                                    is ProfileEffect.Notify -> snackbarHostState.showEmmSnackbar(
                                        message = effect.message.toText(),
                                        tone = when (effect.message) {
                                            ProfileMessage.ExportFailed,
                                            ProfileMessage.ImportFailed,
                                            -> EmmSnackbarTone.Error

                                            else -> EmmSnackbarTone.Success
                                        },
                                    )
                                }
                            }
                        }

                        pendingImportJson?.let { json ->
                            val dialogColors = LocalEmmColors.current
                            AlertDialog(
                                onDismissRequest = { pendingImportJson = null },
                                title = { Text("¿Reemplazar tu data?") },
                                text = { Text("Esto va a borrar todo lo que tengas hoy y poner lo del archivo.") },
                                confirmButton = {
                                    TextButton(
                                        onClick = {
                                            vm.onIntent(ProfileIntent.ImportJson(json))
                                            pendingImportJson = null
                                        },
                                    ) {
                                        Text(
                                            text = "Reemplazar todo",
                                            color = dialogColors.danger,
                                        )
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { pendingImportJson = null }) {
                                        Text("Cancelar")
                                    }
                                },
                            )
                        }

                        ProfileScreen(
                            state = profileState,
                            appVersion = appVersion,
                            isDebug = platform.isDebug,
                            onCategoriesClick = { backStack.add(CategoriesListRoute) },
                            onAccountsClick = { backStack.add(AccountsRoute) },
                            onRecurringClick = { backStack.add(RecurringMovementsRoute) },
                            onAboutClick = { backStack.add(ManifestoRoute(isRevisit = true)) },
                            onExportClick = {
                                if (platform.supportsBackup) vm.onIntent(ProfileIntent.ExportRequested)
                            },
                            onImportClick = { if (platform.supportsBackup) platform.requestImport() },
                            onPrivacyClick = {
                                if (platform.supportsPrivacyPolicy) backStack.add(PrivacyPolicyRoute)
                            },
                            onSignInClick = { backStack.add(AuthRoute) },
                            onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
                            onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
                            onSyncNowClick = { vm.onIntent(ProfileIntent.SyncNow) },
                        )
                    }

                    entry<ReportRoute> {
                        ReportScreen(
                            onBack = { backStack.removeLastOrNull() },
                            onAddTransaction = { backStack.add(AddTransactionRoute) },
                            onShareText = platform.onShareText,
                        )
                    }

                    entry<AddTransactionRoute> {
                        val vm: AddTransactionViewModel = koinViewModel()

                        LaunchedEffect(pendingCategory) {
                            pendingCategory?.let { selectableCategory ->
                                vm.onIntent(AddTransactionIntent.OnNewValueFromOthers(selectableCategory))
                                pendingCategory = null
                            }
                        }

                        AddTransactionScreen(
                            vm = vm,
                            popBackStack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onAddNewCategory = {
                                backStack.add(
                                    CategoryRoute(
                                        propagateToTransaction = true,
                                    ),
                                )
                            },
                            onAddNewAccount = {
                                backStack.add(AddAccountRoute)
                            },
                        )
                    }

                    entry<EditTransactionRoute> { key ->
                        EditTransaction(
                            transactionId = key.transactionId,
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onAddNewAccount = { backStack.add(AddAccountRoute) },
                        )
                    }

                    entry<CategoryRoute> { key ->
                        AddCategoryScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            onCategorySave = { created ->
                                if (key.propagateToTransaction) {
                                    pendingCategory = SelectableCategory(
                                        categoryId = created.categoryId,
                                        name = created.name,
                                        icon = AppIconCatalog.findById(created.icon),
                                        color = findById(created.color),
                                        categoryType = created.categoryType,
                                    )
                                    backStack.popToTransactionScreen()
                                } else {
                                    showRootMessage("Categoría «${created.name}» creada")
                                    backStack.removeLastOrNull()
                                }
                            },
                            vm = koinViewModel(
                                parameters = { parametersOf(key.initialType, key.initialName) },
                            ),
                        )
                    }

                    entry<AddAccountRoute> {
                        AddAccountScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                        )
                    }

                    entry<RecurringMovementsRoute> {
                        RecurringMovementsEntry(
                            onNavigateToAddEdit = { id -> backStack.add(AddEditRecurringMovementRoute(id)) },
                            onShowError = showRootMessage,
                        )
                    }

                    entry<AddEditRecurringMovementRoute> { key ->
                        AddEditRecurringMovementScreen(
                            onBack = { backStack.removeLastOrNull() },
                            snackbarHostState = snackbarHostState,
                            id = key.id,
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeEntry(
    navigateToAll: () -> Unit,
    navigateToAdd: () -> Unit,
    navigateToEdit: (String) -> Unit,
    navigateToReport: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    val vm: HomeViewModel = koinViewModel()
    var confirmSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                HomeEffect.CloseConfirmSheet -> confirmSheetOpen = false
                is HomeEffect.ShowError -> snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    HomeScreen(
        homeViewModel = vm,
        confirmSheetOpen = confirmSheetOpen,
        onConfirmSheetOpenChange = { confirmSheetOpen = it },
        navigateToAll = navigateToAll,
        navigateToAdd = navigateToAdd,
        navigateToEdit = navigateToEdit,
        navigateToReport = navigateToReport,
    )
}

@Composable
private fun RecurringMovementsEntry(onNavigateToAddEdit: (String?) -> Unit, onShowError: (String) -> Unit) {
    val vm: RecurringMovementsViewModel = koinViewModel()
    val recurringState by vm.state.collectAsStateWithLifecycle()
    val currentNavigate by rememberUpdatedState(onNavigateToAddEdit)
    val currentShowError by rememberUpdatedState(onShowError)

    LaunchedEffect(vm) {
        vm.effect.collect { effect ->
            when (effect) {
                is RecurringMovementsEffect.NavigateToAddEdit -> currentNavigate(effect.id)
                is RecurringMovementsEffect.ShowError -> currentShowError(effect.message)
            }
        }
    }

    RecurringMovementsScreen(state = recurringState, onIntent = vm::onIntent)
}
