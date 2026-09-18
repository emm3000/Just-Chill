package com.emm.justchill.hh.shared

import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import com.emm.justchill.core.ui.navigation.CaptureRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data class ManifestoRoute(val isRevisit: Boolean = false) : AppRoute

@Serializable
data object PrivacyPolicyRoute : AppRoute

@Serializable
data object SeeTransactionRoute : BottomBarRoute

@Serializable
data object ProfileRoute : BottomBarRoute

@Serializable
data object AuthRoute : AppRoute

@Serializable
data class AddTransactionRoute(
    val preselectedAccountId: String? = null,
    val preselectedCategoryId: String? = null,
    val preselectedType: TransactionType? = null,
) : CaptureRoute

@Serializable
data class EditTransactionRoute(val transactionId: String) : CaptureRoute

@Serializable
data object CategoriesListRoute : AppRoute

@Serializable
data class CategoryRoute(
    val initialType: CategoryType = CategoryType.Spend,
    val initialName: String = "",
    val propagateToTransaction: Boolean = false,
) : AppRoute

@Serializable
data object RecurringMovementsRoute : AppRoute

@Serializable
data class AddEditRecurringMovementRoute(val id: String? = null) : AppRoute

@Serializable
data object ReportRoute : BottomBarRoute

@Serializable
data object LoansRoute : AppRoute

@Serializable
data class PersonLoansRoute(val personKey: String) : AppRoute

@Serializable
data class LoanDetailRoute(val loanId: String) : AppRoute

@Serializable
data class AddEditLoanRoute(val loanId: String? = null) : AppRoute

val hhRoutes: List<KClass<out AppRoute>> = listOf(
    ManifestoRoute::class,
    PrivacyPolicyRoute::class,
    SeeTransactionRoute::class,
    ProfileRoute::class,
    AuthRoute::class,
    AddTransactionRoute::class,
    EditTransactionRoute::class,
    CategoriesListRoute::class,
    CategoryRoute::class,
    RecurringMovementsRoute::class,
    AddEditRecurringMovementRoute::class,
    ReportRoute::class,
    LoansRoute::class,
    PersonLoansRoute::class,
    LoanDetailRoute::class,
    AddEditLoanRoute::class,
)
