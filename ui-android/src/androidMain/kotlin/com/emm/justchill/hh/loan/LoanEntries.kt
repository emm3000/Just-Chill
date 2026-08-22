package com.emm.justchill.hh.loan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.LoansRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.PersonLoansRoute
import com.emm.justchill.hh.shared.rememberAppNavigator
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

// LoansEffect.NavigateToAddLoan and PersonLoansEffect.NavigateToAddPayment/NavigateToEditLoan have
// no destination yet (E05-08/E05-09): the branches below are no-ops on purpose.
fun EntryProviderScope<NavKey>.loanEntries(bindings: NavHostBindings) {
    entry<LoansRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: LoansViewModel = koinViewModel()
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is LoansEffect.NavigateToPerson -> nav.push(PersonLoansRoute(effect.personKey))
                    LoansEffect.NavigateToAddLoan -> Unit
                }
            }
        }

        LoansScreen(
            state = state,
            onIntent = vm::onIntent,
            onBack = { nav.pop() },
            modifier = Modifier.fillMaxSize(),
        )
    }

    entry<PersonLoansRoute> { key ->
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        val vm: PersonLoansViewModel = koinViewModel { parametersOf(key.personKey) }
        val state by vm.state.collectAsStateWithLifecycle()

        LaunchedEffect(vm) {
            vm.effect.collect { effect ->
                when (effect) {
                    is PersonLoansEffect.ShowError -> bindings.showMessage(effect.message)
                    is PersonLoansEffect.NavigateToAddPayment -> Unit
                    is PersonLoansEffect.NavigateToEditLoan -> Unit
                }
            }
        }

        PersonLoansScreen(
            state = state,
            onIntent = vm::onIntent,
            onBack = { nav.pop() },
            modifier = Modifier.fillMaxSize(),
        )
    }
}
