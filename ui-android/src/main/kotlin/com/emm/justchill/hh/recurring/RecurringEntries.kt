package com.emm.justchill.hh.recurring

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddEditRecurringMovementRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.RecurringMovementsRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel

fun EntryProviderScope<NavKey>.recurringEntries(bindings: NavHostBindings) {
    entry<RecurringMovementsRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        RecurringMovementsEntry(
            onNavigateToAddEdit = { id -> nav.push(AddEditRecurringMovementRoute(id)) },
            onShowError = bindings.showMessage,
        )
    }

    entry<AddEditRecurringMovementRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        AddEditRecurringMovementScreen(
            onBack = { nav.pop() },
            snackbarHostState = bindings.snackbarHostState,
            id = key.id,
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
