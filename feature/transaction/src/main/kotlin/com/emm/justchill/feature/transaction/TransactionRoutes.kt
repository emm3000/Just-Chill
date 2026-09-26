package com.emm.justchill.feature.transaction

import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import com.emm.justchill.core.ui.navigation.CaptureRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
data object SeeTransactionRoute : BottomBarRoute

@Serializable
data class AddTransactionRoute(
    val preselectedAccountId: String? = null,
    val preselectedCategoryId: String? = null,
    val preselectedType: TransactionType? = null,
) : CaptureRoute

@Serializable
data class EditTransactionRoute(val transactionId: String) : CaptureRoute

val transactionRoutes: List<KClass<out AppRoute>> = listOf(
    SeeTransactionRoute::class,
    AddTransactionRoute::class,
    EditTransactionRoute::class,
)
