package com.emm.justchill.hh.shared

import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.core.ui.navigation.BottomBarRoute
import com.emm.justchill.core.ui.navigation.CaptureRoute
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

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

val hhRoutes: List<KClass<out AppRoute>> = listOf(
    PrivacyPolicyRoute::class,
    SeeTransactionRoute::class,
    ProfileRoute::class,
    AuthRoute::class,
    AddTransactionRoute::class,
    EditTransactionRoute::class,
)
