package com.emm.justchill.core

import android.content.Context
import androidx.lifecycle.ViewModel
import com.emm.domain.category.CategoryType
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import com.russhwolf.settings.SettingsInitializer
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.BeanDefinition
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Whole-graph Koin resolution test — the CI guard for the project's highest runtime risk.
 *
 * Since the commonMain DI dedup, every binding except the platform seam lives in one place and is
 * assembled by `appModules(platformModule)`. A missing, mistyped or wrongly qualified binding there
 * compiles cleanly AND survives `assembleDevDebug`: it only surfaces as a crash the moment a user
 * navigates to the affected screen. This test closes that gap by building the real graph against
 * [testPlatformModule] and resolving EVERY definition it contains.
 *
 * Deliberately NOT covered: `bootstrapAppGraph`. Its three resolutions (the appScope
 * `CoroutineScope`, `ClaimLocalDataOnAuthenticationUseCase`, `SyncOrchestrator`) are all swept by
 * [every definition in the app graph resolves], but calling it would also start the orchestrator's
 * collectors, and the Android `resumeEvents()` actual needs `ProcessLifecycleOwner` — an Android
 * runtime this host test does not have.
 */
@OptIn(KoinInternalApi::class)
class AppGraphKoinTest {

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        // supabaseModule's `install(Auth)` builds a SettingsSessionManager from multiplatform-
        // settings' no-arg Settings(), whose Android implementation takes its Context from an
        // androidx.startup Initializer that only runs inside a real app. SettingsInitializer.create
        // is the library's documented hook for supplying that Context from tests; without it the
        // SupabaseClient single cannot be created and 22 of the 105 bindings (all of auth and sync)
        // would be untestable. A relaxed mock is enough — nothing here reads or writes preferences.
        SettingsInitializer().create(mockk<Context>(relaxed = true))

        // Every ViewModel creates a viewModelScope on Dispatchers.Main at construction. Standard
        // (not Unconfined) on purpose: init-block coroutines stay queued and never run, so this
        // test measures WIRING only and cannot flake on database contents or network reachability.
        Dispatchers.setMain(StandardTestDispatcher())
        koin = koinApplication { modules(appModules(testPlatformModule)) }.koin
    }

    @After
    fun tearDown() {
        koin.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `every definition in the app graph resolves`() {
        val boundTypes: List<Pair<KClass<*>, Qualifier?>> = koin.boundTypes()
        val failures: MutableList<String> = mutableListOf()

        for ((type, qualifier) in boundTypes) {
            // Intentional broad catch: any resolution failure (missing dependency, wrong qualifier,
            // constructor throwing) must be collected so ONE run reports every broken binding
            // instead of stopping at the first.
            @Suppress("TooGenericExceptionCaught")
            try {
                koin.get<Any>(type, qualifier, runtimeParametersFor(type))
            } catch (e: Exception) {
                failures += "${describe(type, qualifier)} -> ${e.rootCauseMessage()}"
            }
        }

        assertTrue(
            failures.isEmpty(),
            "${failures.size} of ${boundTypes.size} bindings failed to resolve:\n" +
                failures.joinToString("\n") { "  - $it" },
        )
        // Floor guard: if the registry sweep ever stops seeing definitions (a Koin internals change,
        // an empty module list) the loop above would pass vacuously. The graph currently exposes
        // ~105 (type, qualifier) pairs, so anything below 80 means the sweep itself broke.
        assertTrue(
            boundTypes.size >= MIN_EXPECTED_BINDINGS,
            "Only ${boundTypes.size} bindings were discovered; the registry sweep looks broken.",
        )
    }

    @Test
    fun `every ViewModel is registered and resolves with its navigation parameters`() {
        val registered: Set<String> = koin.definitions()
            .filter { ViewModel::class.java.isAssignableFrom(it.primaryType.java) }
            .mapNotNull { it.primaryType.simpleName }
            .toSet()

        // Exhaustive by design. A ViewModel missing from the graph resolves to nothing at
        // navigation time, so a NEW ViewModel must be added BOTH to its Koin module and to this
        // list; a ViewModel that disappears from here without a matching deletion is a regression.
        assertEquals(
            EXPECTED_VIEW_MODELS,
            registered.toSortedSet(),
            "The set of ViewModels registered in appModules() changed.",
        )

        // Resolving each one is what a Screen does at navigation time — including the three that
        // take runtime parameters off the nav route.
        registered.forEach { name ->
            val definition = koin.definitions().first { it.primaryType.simpleName == name }
            val viewModel = koin.get<Any>(
                definition.primaryType,
                definition.qualifier,
                runtimeParametersFor(definition.primaryType),
            )
            assertTrue(viewModel is ViewModel, "$name did not resolve to a ViewModel.")
        }
    }

    /**
     * Every distinct (type, qualifier) pair a consumer could ask Koin for: each definition's primary
     * type plus every interface it is `bind`-ed to, since consumers inject the interface.
     */
    private fun Koin.boundTypes(): List<Pair<KClass<*>, Qualifier?>> = definitions()
        .flatMap { definition ->
            (listOf(definition.primaryType) + definition.secondaryTypes).map { it to definition.qualifier }
        }
        .distinct()

    // The instance registry is keyed per bound type, so one definition appears under several keys;
    // distinct() collapses them back to one InstanceFactory per definition (identity equality).
    private fun Koin.definitions(): List<BeanDefinition<*>> = instanceRegistry.instances.values
        .distinct()
        .map { it.beanDefinition }

    private fun runtimeParametersFor(type: KClass<*>): ParametersDefinition? = RUNTIME_PARAMETERS[type]

    private fun describe(type: KClass<*>, qualifier: Qualifier?): String =
        if (qualifier == null) type.simpleName.orEmpty() else "${type.simpleName}(${qualifier.value})"

    /**
     * Koin wraps every resolution failure in one InstanceCreationException per nesting level, so the
     * outermost message only names the consumer. The innermost cause is the actionable one — it
     * names the type that has no definition.
     */
    private fun Exception.rootCauseMessage(): String {
        var root: Throwable = this
        while (root.cause != null && root.cause !== root) {
            root = checkNotNull(root.cause)
        }
        return (root.message ?: root::class.simpleName.orEmpty()).lineSequence().first().trim()
    }

    private companion object {

        const val MIN_EXPECTED_BINDINGS = 80

        val EXPECTED_VIEW_MODELS = sortedSetOf(
            "AccountsViewModel",
            "AddAccountViewModel",
            "AddCategoryViewModel",
            "AddEditRecurringMovementViewModel",
            "AddTransactionViewModel",
            "AuthViewModel",
            "CategoriesViewModel",
            "EditTransactionViewModel",
            "HomeViewModel",
            "ProfileViewModel",
            "RecurringMovementsViewModel",
            "ReportViewModel",
            "SeeTransactionsViewModel",
        )

        /**
         * Representative values for the definitions declared as `viewModel { parameters -> ... }`.
         * They mirror what the nav routes pass in, so the parameter slots are exercised rather than
         * skipped. Each entry is a factory, not a shared holder, so one resolution cannot consume
         * the parameters of another.
         */
        val RUNTIME_PARAMETERS: Map<KClass<*>, ParametersDefinition> = mapOf(
            // Opened pre-filled from the transaction form (chosen type + typed-in name).
            AddCategoryViewModel::class to { parametersOf(CategoryType.Spend, "Test Category") },
            // Transaction id from the edit route.
            EditTransactionViewModel::class to { parametersOf("test-transaction-id") },
            // Nullable template id; a non-null value exercises the edit branch over the create one.
            AddEditRecurringMovementViewModel::class to { parametersOf("test-recurring-id") },
        )
    }
}
