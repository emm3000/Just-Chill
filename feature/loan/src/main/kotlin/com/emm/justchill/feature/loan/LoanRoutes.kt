package com.emm.justchill.feature.loan

import com.emm.justchill.core.ui.navigation.AppRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object LoansRoute : AppRoute

@Serializable
data class PersonLoansRoute(val personKey: String) : AppRoute

@Serializable
data class LoanDetailRoute(val loanId: String) : AppRoute

@Serializable
data class AddEditLoanRoute(val loanId: String? = null) : AppRoute

val loanRoutes: List<KClass<out AppRoute>> = listOf(
    LoansRoute::class,
    PersonLoansRoute::class,
    LoanDetailRoute::class,
    AddEditLoanRoute::class,
)
