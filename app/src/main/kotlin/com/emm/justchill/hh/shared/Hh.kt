package com.emm.justchill.hh.shared

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emm.justchill.BuildConfig
import com.emm.justchill.core.error.toUserMessage
import com.emm.justchill.core.preferences.AppPreferences
import com.emm.justchill.core.sync.SyncOrchestrator
import com.emm.justchill.core.theme.InterFontFamily
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
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val START_TAB: BottomBarRoute = SeeTransactionRoute

@Suppress("CyclomaticComplexMethod")
@Composable
fun Hh(modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val appPrefs: AppPreferences = koinInject()
    val syncOrchestrator: SyncOrchestrator = koinInject()
    val startRoute: NavKey = remember {
        if (appPrefs.firstLaunchSeen) START_TAB else ManifestoRoute()
    }
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(startRoute)
    var pendingCategory by remember { mutableStateOf<SelectableCategory?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val rootScope = rememberCoroutineScope()
    val showRootMessage: (String) -> Unit = { message ->
        rootScope.launch { snackbarHostState.showEmmSnackbar(message) }
    }

    SyncEventsHandler(
        syncOrchestrator = syncOrchestrator,
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
                    onTabClick = { tab -> backStack.switchTab(tab) },
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
                                backStack.replaceAll(START_TAB)
                            }
                        },
                    )
                }

                entry<PrivacyPolicyRoute> {
                    PrivacyPolicyScreen(
                        onBack = { backStack.removeLastOrNull() },
                    )
                }

                entry<AuthRoute> {
                    val context = LocalContext.current
                    AuthScreen(
                        onBack = { backStack.removeLastOrNull() },
                        snackbarHostState = snackbarHostState,
                        onOpenEmailApp = {
                            try {
                                val intent = Intent(Intent.ACTION_MAIN)
                                    .addCategory(Intent.CATEGORY_APP_EMAIL)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                rootScope.launch {
                                    snackbarHostState.showEmmSnackbar(
                                        message = "No encontramos una app de correo en tu teléfono.",
                                        tone = EmmSnackbarTone.Error,
                                    )
                                }
                            }
                        },
                    )
                }

                entry<HomeRoute> {
                    HomeEntry(
                        navigateToAll = dropUnlessResumed {
                            backStack.switchTab(SeeTransactionRoute)
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
                    val context = LocalContext.current
                    val scope = rememberCoroutineScope()
                    var pendingImportJson by remember { mutableStateOf<String?>(null) }
                    // Holds the backup JSON produced by the VM until the SAF picker returns a destination.
                    var pendingExportJson by remember { mutableStateOf<String?>(null) }

                    val exportLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.CreateDocument("application/json"),
                    ) { uri ->
                        val json = pendingExportJson
                        pendingExportJson = null
                        if (uri != null && json != null) {
                            // Platform owns the SAF write; the VM only generated the JSON (commonMain, no IO).
                            val ok = runCatching {
                                context.contentResolver.openOutputStream(uri)?.use { stream ->
                                    stream.bufferedWriter().use { it.write(json) }
                                } != null
                            }.getOrDefault(false)
                            val message = if (ok) ProfileMessage.ExportDone else ProfileMessage.ExportFailed
                            scope.launch {
                                snackbarHostState.showEmmSnackbar(
                                    message = message.toText(),
                                    tone = if (ok) EmmSnackbarTone.Success else EmmSnackbarTone.Error,
                                )
                            }
                        }
                    }

                    val importLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri != null) {
                            val text = context.contentResolver.openInputStream(uri)
                                ?.bufferedReader()
                                ?.use { it.readText() }
                            if (text != null) pendingImportJson = text
                        }
                    }

                    LaunchedEffect(vm) {
                        vm.effect.collect { effect ->
                            when (effect) {
                                is ProfileEffect.ShowError -> snackbarHostState.showEmmSnackbar(
                                    message = effect.error.toUserMessage(),
                                    tone = EmmSnackbarTone.Error,
                                )

                                is ProfileEffect.ExportReady -> {
                                    // VM finished generating the backup; stash it and open the SAF picker.
                                    pendingExportJson = effect.json
                                    exportLauncher.launch(suggestedExportFilename())
                                }

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
                        val colors = LocalEmmColors.current
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
                                        color = colors.danger,
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

                    val profileState by vm.state.collectAsStateWithLifecycle()
                    ProfileScreen(
                        state = profileState,
                        appVersion = BuildConfig.VERSION_NAME,
                        isDebug = BuildConfig.DEBUG,
                        onCategoriesClick = { backStack.add(CategoriesListRoute) },
                        onAccountsClick = { backStack.add(AccountsRoute) },
                        onRecurringClick = { backStack.add(RecurringMovementsRoute) },
                        onAboutClick = { backStack.add(ManifestoRoute(isRevisit = true)) },
                        onExportClick = { vm.onIntent(ProfileIntent.ExportRequested) },
                        onImportClick = { importLauncher.launch(arrayOf("application/json")) },
                        onPrivacyClick = { backStack.add(PrivacyPolicyRoute) },
                        onSignInClick = { backStack.add(AuthRoute) },
                        onSignOutClick = { vm.onIntent(ProfileIntent.SignOut) },
                        onDeleteAccountClick = { vm.onIntent(ProfileIntent.DeleteAccount) },
                        onSyncNowClick = { vm.onIntent(ProfileIntent.SyncNow) },
                    )
                }

                entry<ReportRoute> {
                    val context = LocalContext.current
                    ReportScreen(
                        onBack = { backStack.removeLastOrNull() },
                        onAddTransaction = { backStack.add(AddTransactionRoute) },
                        onShareText = { text ->
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, text)
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir reporte"))
                        },
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
    val currentNavigate by androidx.compose.runtime.rememberUpdatedState(onNavigateToAddEdit)
    val currentShowError by androidx.compose.runtime.rememberUpdatedState(onShowError)

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

private fun ProfileMessage.toText(): String = when (this) {
    ProfileMessage.SessionClosed -> "Sesión cerrada. Tus datos siguen en este teléfono."
    ProfileMessage.AccountDeleted -> "Cuenta eliminada. Tus datos siguen en este teléfono."
    ProfileMessage.ExportDone -> "Listo, tu data está guardada."
    ProfileMessage.ExportFailed -> "No pude exportar — capaz no hay espacio en tu celu?"
    is ProfileMessage.ImportDone -> "Listo — $transactions movimientos importados."
    ProfileMessage.ImportFailed -> "No pude importar el archivo — capaz está dañado."
}

private fun suggestedExportFilename(): String {
    val date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    return "justchill-backup-$date.json"
}

/**
 * Replaces the entire back stack with [route]. Used for auth transitions and logout.
 */
private fun NavBackStack<NavKey>.replaceAll(route: NavKey) {
    clear()
    add(route)
}

/**
 * Switches to a bottom-bar tab using the "exit through home" pattern:
 * back stack always starts at [START_TAB], with the selected tab on top (if different).
 */
private fun NavBackStack<NavKey>.switchTab(target: BottomBarRoute) {
    clear()
    add(START_TAB)
    if (target != START_TAB) add(target)
}

private data class BottomTab(
    val route: BottomBarRoute?, // null = add pseudo-tab
    val label: String,
    val icon: ImageVector,
    val isAdd: Boolean = false,
)

private val BOTTOM_TABS = listOf(
    BottomTab(HomeRoute, "Inicio", Icons.Outlined.Home),
    BottomTab(SeeTransactionRoute, "Ver", Icons.AutoMirrored.Outlined.List),
    BottomTab(null, "Agregar", Icons.Outlined.Add, isAdd = true),
    BottomTab(AccountsRoute, "Cuentas", Icons.Outlined.AccountBalanceWallet),
    BottomTab(ProfileRoute, "Perfil", Icons.Outlined.Person),
)

@Composable
private fun HhBottomBar(current: BottomBarRoute?, onTabClick: (BottomBarRoute) -> Unit, onAddClick: () -> Unit) {
    val colors = LocalEmmColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.bg)
            .navigationBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .size(1.dp)
                .background(colors.border),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            BOTTOM_TABS.forEach { tab ->
                if (tab.isAdd) {
                    AddBottomBarItem(
                        label = tab.label,
                        onClick = dropUnlessResumed(block = onAddClick),
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    val isActive = tab.route == current
                    RegularBottomBarItem(
                        label = tab.label,
                        icon = tab.icon,
                        isActive = isActive,
                        onClick = dropUnlessResumed { tab.route?.let(onTabClick) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun RegularBottomBarItem(
    label: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalEmmColors.current
    val tint = if (isActive) colors.textPrimary else colors.textDisabled
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = FontWeight.W500,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun AddBottomBarItem(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalEmmColors.current
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(colors.accent),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.size(3.dp))
        Text(
            text = label,
            color = colors.accent,
            fontSize = 10.sp,
            fontWeight = FontWeight.W600,
            fontFamily = InterFontFamily,
            letterSpacing = 0.1.sp,
            maxLines = 1,
        )
    }
}

/**
 * Pop intermediate routes (`CategoryRoute` and any legacy in-between) until
 * the transaction screen is at the top, so it receives `pendingCategory`
 * via its `LaunchedEffect` and the user lands back where they were.
 */
private fun NavBackStack<NavKey>.popToTransactionScreen() {
    while (isNotEmpty() && last() !is AddTransactionRoute && last() !is EditTransactionRoute) {
        removeLastOrNull()
    }
}
