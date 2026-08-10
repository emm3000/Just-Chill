package com.emm.justchill.hh.shared

import androidx.navigation3.runtime.NavKey
import com.emm.domain.category.CategoryType
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.json.Json
import org.junit.Test
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Guards the landmine documented in `NavSavedStateConfiguration.kt`: a route the nav host can push but
 * that nobody registered in [navSavedStateConfiguration]. Kotlin/Native has no reflective serializer
 * discovery, so such a route crashes `rememberNavBackStack` on process-death restore — a failure the
 * compiler, `assembleDevDebug` and the iOS compile all wave through.
 *
 * The guard is automatic rather than a hand-copied list: [AppRoute] is sealed, so [concreteRoutesUnder]
 * enumerates the whole route set by reflection and the first test fails until a new route gets a sample.
 *
 * What this does NOT cover: the `SavedState`/`Bundle` encoder path that actually writes the back stack
 * (that needs Robolectric or a device). It covers the `serializersModule` lookup and the serializers it
 * hands back — and the documented crash IS a `serializersModule` lookup failure, so the gap is narrow.
 *
 * Lives in `androidHostTest` rather than `commonTest` because `KClass.sealedSubclasses` is JVM-only.
 */
class NavSavedStateConfigurationTest {

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

    // getPolymorphic(baseClass, value) is the exact lookup SavedState performs on restore, and it is
    // still marked experimental. Opting in here keeps the test honest instead of asserting on a proxy.
    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `every route is registered for NavKey polymorphism`() {
        val serializersModule = navSavedStateConfiguration.serializersModule

        samples.forEach { (routeClass, route) ->
            val name: String = routeClass.render()
            assertNotNull(
                serializersModule.getPolymorphic(NavKey::class, route),
                "$name is not registered for NavKey polymorphism, so rememberNavBackStack will crash " +
                    "on process-death restore. Add `subclass($name::class, $name.serializer())` to " +
                    "navSavedStateConfiguration in NavSavedStateConfiguration.kt.",
            )
        }
    }

    @Test
    fun `every route survives a polymorphic round trip`() {
        val json = Json { serializersModule = navSavedStateConfiguration.serializersModule }
        val serializer = PolymorphicSerializer(NavKey::class)

        samples.forEach { (routeClass, route) ->
            val encoded: String = json.encodeToString(serializer, route)
            val decoded: NavKey = json.decodeFromString(serializer, encoded)

            assertEquals(
                route,
                decoded,
                "${routeClass.render()} does not survive a polymorphic round trip. Encoded as $encoded.",
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
