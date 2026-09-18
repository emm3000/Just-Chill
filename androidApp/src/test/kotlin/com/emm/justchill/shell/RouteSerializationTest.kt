package com.emm.justchill.shell

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.transaction.TransactionType
import com.emm.justchill.core.ui.navigation.AppRoute
import com.emm.justchill.feature.account.AccountsRoute
import com.emm.justchill.feature.account.AddAccountRoute
import com.emm.justchill.feature.account.accountRoutes
import com.emm.justchill.feature.category.CategoriesListRoute
import com.emm.justchill.feature.category.CategoryRoute
import com.emm.justchill.feature.category.categoryRoutes
import com.emm.justchill.hh.shared.AddEditLoanRoute
import com.emm.justchill.hh.shared.AddEditRecurringMovementRoute
import com.emm.justchill.hh.shared.AddTransactionRoute
import com.emm.justchill.hh.shared.AuthRoute
import com.emm.justchill.hh.shared.EditTransactionRoute
import com.emm.justchill.hh.shared.LoanDetailRoute
import com.emm.justchill.hh.shared.LoansRoute
import com.emm.justchill.hh.shared.ManifestoRoute
import com.emm.justchill.hh.shared.PersonLoansRoute
import com.emm.justchill.hh.shared.PrivacyPolicyRoute
import com.emm.justchill.hh.shared.ProfileRoute
import com.emm.justchill.hh.shared.RecurringMovementsRoute
import com.emm.justchill.hh.shared.ReportRoute
import com.emm.justchill.hh.shared.SeeTransactionRoute
import com.emm.justchill.hh.shared.hhRoutes
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSerializationTest {

    @Test
    fun `every registered route has a sample`() {
        val registered: Set<KClass<out AppRoute>> = registries.flatten().toSet()
        val missing: Set<KClass<out AppRoute>> = registered - samples.keys
        val extra: Set<KClass<out AppRoute>> = samples.keys - registered

        assertTrue(
            missing.isEmpty() && extra.isEmpty(),
            "samples must hold exactly one instance per registered AppRoute.\n" +
                "  missing (in a route registry but not sampled here): ${missing.render()}\n" +
                "  extra (sampled here but in no route registry): ${extra.render()}",
        )
    }

    @Test
    fun `every route survives a round trip through the Android reflection serializer`() {
        // The exact pair rememberNavBackStack(vararg NavKey) builds internally on Android.
        val serializer: KSerializer<NavBackStack<NavKey>> =
            NavBackStackSerializer(elementSerializer = NavKeySerializer())

        samples.forEach { (routeClass, route) ->
            val encoded: String = Json.encodeToString(serializer, NavBackStack<NavKey>(route))
            val decoded: NavBackStack<NavKey> = Json.decodeFromString(serializer, encoded)

            assertEquals(
                listOf<NavKey>(route),
                decoded.toList(),
                "${routeClass.render()} does not survive the reflective NavKey round trip, so " +
                    "rememberNavBackStack will fail on process-death restore. Encoded as $encoded.",
            )
        }
    }

    private val registries: List<List<KClass<out AppRoute>>> =
        listOf(hhRoutes, accountRoutes, categoryRoutes)

    /**
     * Data classes get NON-DEFAULT field values on purpose: a field whose serializer is broken then
     * fails the round trip instead of hiding behind a default.
     */
    private val samples: Map<KClass<out AppRoute>, AppRoute> = listOf<AppRoute>(
        ManifestoRoute(isRevisit = true),
        PrivacyPolicyRoute,
        SeeTransactionRoute,
        AccountsRoute,
        ProfileRoute,
        AuthRoute,
        AddTransactionRoute(
            preselectedAccountId = "acc-1",
            preselectedCategoryId = "cat-1",
            preselectedType = TransactionType.Income,
        ),
        EditTransactionRoute(transactionId = "tx-1"),
        AddAccountRoute,
        CategoriesListRoute,
        CategoryRoute(
            initialType = CategoryType.Income,
            initialName = "Sueldo",
            propagateToTransaction = true,
        ),
        RecurringMovementsRoute,
        AddEditRecurringMovementRoute(id = "rec-1"),
        ReportRoute,
        LoansRoute,
        PersonLoansRoute(personKey = "person-1"),
        LoanDetailRoute(loanId = "loan-1"),
        AddEditLoanRoute(loanId = "loan-1"),
    ).associateBy { it::class }

    private fun KClass<*>.render(): String = simpleName ?: qualifiedName ?: toString()

    private fun Set<KClass<*>>.render(): String =
        if (isEmpty()) "none" else joinToString(separator = ", ") { it.render() }
}
