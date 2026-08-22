package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavBackStackSerializer
import androidx.navigation3.runtime.serialization.NavKeySerializer
import com.emm.domain.category.CategoryType
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSerializationTest {

    @Test
    fun `every concrete route has a sample`() {
        val reflected: Set<KClass<out AppRoute>> = concreteRoutesUnder(AppRoute::class)
        val missing: Set<KClass<out AppRoute>> = reflected - samples.keys
        val extra: Set<KClass<out AppRoute>> = samples.keys - reflected

        assertTrue(
            missing.isEmpty() && extra.isEmpty(),
            "samples must hold exactly one instance per concrete AppRoute.\n" +
                "  missing (declared as an AppRoute but not sampled here): ${missing.render()}\n" +
                "  extra (sampled here but no longer a concrete AppRoute): ${extra.render()}",
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

    /**
     * Data classes get NON-DEFAULT field values on purpose: a field whose serializer is broken then
     * fails the round trip instead of hiding behind a default.
     */
    private val samples: Map<KClass<out AppRoute>, AppRoute> = listOf<AppRoute>(
        ManifestoRoute(isRevisit = true),
        PrivacyPolicyRoute,
        HomeRoute,
        SeeTransactionRoute,
        AccountsRoute,
        ProfileRoute,
        AuthRoute,
        AddTransactionRoute,
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

    private fun concreteRoutesUnder(root: KClass<out AppRoute>): Set<KClass<out AppRoute>> {
        val concrete: MutableSet<KClass<out AppRoute>> = mutableSetOf()
        for (subclass in root.sealedSubclasses) {
            if (subclass.isSealed || subclass.isAbstract) {
                concrete += concreteRoutesUnder(subclass)
            } else {
                concrete += subclass
            }
        }
        return concrete
    }

    private fun KClass<*>.render(): String = simpleName ?: qualifiedName ?: toString()

    private fun Set<KClass<*>>.render(): String =
        if (isEmpty()) "none" else joinToString(separator = ", ") { it.render() }
}
