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

/**
 * Guards the one invariant `AppNavHost`'s back stack depends on: every concrete [AppRoute] is
 * `@Serializable` AND its serializer is reachable the way the Android reflection path reaches it.
 *
 * `rememberNavBackStack(startRoute)` — the Android-only 1-arg overload — persists the stack with
 * `NavBackStackSerializer(elementSerializer = NavKeySerializer())`. `NavKeySerializer` writes
 * `value::class.java.name` and reads it back through `Class.forName(name).kotlin.serializer()`, so a
 * route that is not `@Serializable` (or whose fields are not) blows up on process-death restore and
 * nowhere else — the compiler, `assembleDevDebug` and lint all wave it through. This test drives that
 * exact serializer pair instead of a stand-in, so what passes here is what the host runs.
 *
 * The guard is automatic rather than a hand-copied list: [AppRoute] is sealed, so [concreteRoutesUnder]
 * enumerates the whole route set by reflection and the first test fails until a new route gets a sample.
 *
 * What this does NOT cover: the `SavedState`/`Bundle` encoder path that actually writes the back stack
 * (that needs Robolectric or a device). [Json] stands in as the format. The failure mode this guards —
 * an unresolvable or missing element serializer — is format-independent, so the gap is narrow.
 *
 * Lives in `androidHostTest` rather than a shared source set because `KClass.sealedSubclasses` and the
 * `Class.forName` path under test are both JVM-only. So is this module (ADR 005).
 */
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
     * One instance per concrete [AppRoute]. Data classes get NON-DEFAULT field values on purpose: a
     * field whose serializer is broken then fails the round trip instead of hiding behind a default.
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
        // initialType deliberately exercises CategoryType, an enum owned by the :domain module.
        CategoryRoute(
            initialType = CategoryType.Income,
            initialName = "Sueldo",
            propagateToTransaction = true,
        ),
        RecurringMovementsRoute,
        AddEditRecurringMovementRoute(id = "rec-1"),
        ReportRoute,
    ).associateBy { it::class }

    /** Every concrete (non-sealed, non-abstract) route reachable from [root] through sealed subclasses. */
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
