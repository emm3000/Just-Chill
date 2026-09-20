package com.emm.justchill.feature.recurring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.ui.category.SelectableCategory
import com.emm.justchill.core.ui.navigation.AppNavigator
import com.emm.justchill.core.ui.navigation.NavHostBindings
import com.emm.justchill.core.ui.navigation.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

fun EntryProviderScope<NavKey>.recurringEntries(
    bindings: NavHostBindings,
    pendingCategory: () -> SelectableCategory?,
    onPendingCategoryConsumed: () -> Unit,
    onAddNewCategory: (AppNavigator, CategoryType) -> Unit,
) {
    entry<RecurringMovementsRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        RecurringMovementsEntry(
            onNavigateToAddEdit = { id -> nav.push(AddEditRecurringMovementRoute(id)) },
            onShowError = bindings.showMessage,
        )
    }

    entry<AddEditRecurringMovementRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: AddEditRecurringMovementViewModel = koinViewModel(parameters = { parametersOf(key.id) })

        LaunchedEffect(pendingCategory()) {
            pendingCategory()?.let { selectableCategory ->
                vm.onIntent(AddEditRecurringMovementIntent.OnNewValueFromOthers(selectableCategory))
                onPendingCategoryConsumed()
            }
        }

        AddEditRecurringMovementScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            onAddNewCategory = { categoryType -> onAddNewCategory(nav, categoryType) },
            vm = vm,
        )
    }
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
