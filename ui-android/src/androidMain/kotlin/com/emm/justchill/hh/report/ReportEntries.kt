package com.emm.justchill.hh.report

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AppNavigator
import com.emm.justchill.hh.shared.NavHostBindings
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.rememberAppNavigator

fun EntryProviderScope<NavKey>.reportEntries(bindings: NavHostBindings) {
    entry<ReportRoute> {
        val nav: AppNavigator = rememberAppNavigator(bindings.backStack)
        ReportScreen(
            onBack = { nav.pop() },
            onAddTransaction = { nav.push(AddTransactionRoute) },
            onShareText = bindings.platform.onShareText,
        )
    }
}
