package com.emm.justchill.hh.seetransactions

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.rememberAppNavigator

fun EntryProviderScope<NavKey>.seeTransactionsEntries(bindings: NavHostBindings) {
    entry<SeeTransactionRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        SeeTransactionsScreen(
            onEditTransaction = { id ->
                nav.push(EditTransactionRoute(id))
            },
        )
    }
}
