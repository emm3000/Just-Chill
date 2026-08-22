package com.emm.justchill.core

import android.content.Context
import androidx.lifecycle.ViewModel
import com.emm.domain.category.CategoryType
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.loan.AddEditLoanViewModel
import com.emm.justchill.hh.loan.PersonLoansViewModel
import com.emm.justchill.hh.recurring.AddEditRecurringMovementViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import com.russhwolf.settings.SettingsInitializer
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
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
import org.koin.dsl.module
import java.lang.reflect.Field
import kotlin.reflect.KClass
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Whole-graph Koin resolution test — the CI guard for the project's highest runtime risk.
 *
 * Since the commonMain DI dedup, every binding except the platform seam lives in one place and is
 * assembled by `appModules(platformModule)`. A missing, mistyped or wrongly qualified binding there
 * compiles cleanly AND survives `assembleDevDebug`: it only surfaces as a crash the moment a user
 * navigates to the affected screen. This test closes that gap by building the real graph against
 * [testPlatformModule] and resolving EVERY definition it contains.
 *
 * Deliberately NOT *called*: `bootstrapAppGraph` itself, because that would start the orchestrator's
 * collector and the Android `resumeEvents()` / `backgroundEvents()` actuals need
 * `ProcessLifecycleOwner` — an Android runtime this host test does not have. Its resolution
 * is covered instead by [every single bootstrapAppGraph resolves is bound], which resolves it
 * the way it does. Resolving is safe because both lifecycle actuals are `callbackFlow` builders:
 * nothing touches `ProcessLifecycleOwner` until something collects.
 *
 * ### The boundary, stated so nobody assumes past it
 *
 * The graph here is `appModules(testPlatformModule)`. The REAL platform modules —
 * `androidPlatformModule` in `:androidApp`, `iosPlatformModule` in `iosMain` — are not on this
 * source set's classpath and are never loaded, so nothing in this file says anything about what
 * they bind. A binding only they carry is guarded where it lives: `AndroidPlatformModuleTest`
 * (`:androidApp`) does that for [CommitHash], which `AppNavHost` resolves at launch. An
 * assertion written here against [testPlatformModule]'s own literal would only be the fixture
 * checking itself — that is exactly what this test used to do.
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
        // SupabaseClient single cannot be created and every auth binding would be untestable.
        // A relaxed mock is enough — nothing here reads or writes preferences.
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
        // an empty module list) the loop above would pass vacuously.
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
     * The one definition `bootstrapAppGraph` resolves BY TYPE, resolved exactly the way it does.
     *
     * The sweep above proves every *bound* definition resolves. It cannot notice one that was never
     * bound at all — there is nothing in the registry to iterate over — and `bootstrapAppGraph` is
     * precisely where that gap bites: the orchestrator is resolved behind a kill switch that is
     * off today, so deleting its binding would compile, keep this whole suite green, and crash
     * on the day the flag flips. Naming it here is what turns a deleted binding red now.
     *
     * No assertion body on purpose: a missing or unresolvable definition throws out of `get`, which
     * is the failure this test exists to produce.
     */
    @Test
    fun `every single bootstrapAppGraph resolves is bound`() {
        koin.get<BackupOrchestrator>()
    }

    /**
     * The backup port and the orchestrator are ONE instance, not two.
     *
     * `backupModule` publishes [BackupController] as a secondary type of the orchestrator's `single`.
     * Written instead as a second definition — a `single<BackupController> { BackupOrchestrator(…) }`
     * beside the first, or the same class bound as a `factory` — every other test in this file stays
     * green: both types resolve, the sweep is happy, and the ViewModel gets a perfectly valid
     * controller. It is just not the one `bootstrapAppGraph` called `start()` on, so its request
     * channel has no consumer and its status flow never moves. A manual backup would then do
     * *nothing*, silently, which is the exact failure shape ADR 009 hard constraint 4 forbids.
     *
     * `assertSame`, not `assertEquals`: two orchestrators built from the same graph are not equal to
     * each other, but a `factory` would produce two distinct instances that are also not equal, and
     * only identity separates "the port IS the running orchestrator" from "the port resolves".
     */
    @Test
    fun `the backup controller port is the orchestrator single, not a second instance`() {
        assertSame(koin.get<BackupOrchestrator>(), koin.get<BackupController>())
    }

    /**
     * Every `Clock` and `TimeZone` a graph-built class holds is the instance the graph BOUND.
     *
     * Resolution succeeding is not the same as resolution being correct, and the two tests above
     * only prove the first. A class can resolve perfectly while holding a clock nobody injected:
     * `sharedModule` binds `Clock.System`, and a Kotlin default of `Clock.System` produces an
     * indistinguishable value — so the wiring can be wrong and every assertion in this file stays
     * green. That is not hypothetical. `ProfileModule` omitted `clock = get()` for the whole life
     * of the class and nothing here noticed; it was found by reading, not by failing.
     *
     * The two hand-written blocks are where this can go wrong, because they list arguments by hand
     * and a hand-written list can forget one: `profileModule`'s `viewModel { }` and
     * `transactionModule`'s parametrised `EditTransactionViewModel` block. The constructor DSL
     * cannot forget. The sweep is not limited to those two, though — it checks every definition, so
     * converting any class to a hand-written block later inherits the guard for free.
     *
     * Binding sentinels is what makes the difference observable: a clock at a fixed instant and a
     * zone the machine is not in. `assertSame`, not `assertEquals` — two `Clock.System` references
     * are equal and only identity separates "injected" from "defaulted".
     *
     * Scoped to `com.emm.` classes on purpose: third-party singles in the graph (Supabase, Ktor)
     * legitimately hold clocks of their own that this project does not bind.
     */
    @Test
    fun `every graph-built class holds the Clock and TimeZone the graph bound`() {
        val boundClock = object : Clock {
            override fun now(): Instant = Instant.parse("2026-08-11T15:04:05Z")
        }
        val boundZone: TimeZone = TimeZone.of("Asia/Karachi")

        val overridden: Koin = koinApplication {
            modules(
                appModules(testPlatformModule) + module {
                    factory<Clock> { boundClock }
                    factory { boundZone }
                },
            )
        }.koin

        // Resolution needs the graph; reading the fields afterwards does not, so the graph is closed
        // as soon as the sweep has collected them.
        val timeFields: List<Pair<Any, Field>> = try {
            overridden.ownTimeFields()
        } finally {
            overridden.close()
        }

        val mismatches: List<String> = timeFields
            .filter { (owner, field) -> field.get(owner) !== expectedFor(field, boundClock, boundZone) }
            .map { (owner, field) ->
                "${owner::class.java.simpleName}.${field.name} holds " +
                    "${field.get(owner)?.let { it::class.java.name }} instead of the bound instance"
            }

        assertTrue(
            mismatches.isEmpty(),
            "${mismatches.size} class(es) did not receive the bound Clock/TimeZone — the Koin block " +
                "that builds them passes a default or its own instance instead of get():\n" +
                mismatches.joinToString("\n") { "  - $it" },
        )
        // Floor guard, same reasoning as the sweep above: if reflection stops finding fields (a
        // rename, a Kotlin change to how constructor vals are stored) the check passes vacuously.
        assertTrue(
            timeFields.size >= MIN_EXPECTED_TIME_FIELDS,
            "Only ${timeFields.size} Clock/TimeZone fields were inspected; the reflection sweep looks broken.",
        )
    }

    /**
     * Every `Clock`/`TimeZone` field held by a `com.emm.` object the graph can build, paired with
     * the instance that holds it. Third-party singles are skipped: Supabase and Ktor carry clocks
     * of their own that this project does not bind and has no business asserting on.
     */
    private fun Koin.ownTimeFields(): List<Pair<Any, Field>> = definitions().flatMap { definition ->
        val instance: Any = get(
            definition.primaryType,
            definition.qualifier,
            runtimeParametersFor(definition.primaryType),
        )
        if (!instance::class.java.name.startsWith(OWN_PACKAGE_PREFIX)) {
            emptyList()
        } else {
            instance::class.java.declaredFields
                .filter { it.type == Clock::class.java || it.type == TimeZone::class.java }
                .onEach { it.isAccessible = true }
                .map { instance to it }
        }
    }

    private fun expectedFor(field: Field, boundClock: Clock, boundZone: TimeZone): Any =
        if (field.type == Clock::class.java) boundClock else boundZone

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

        const val OWN_PACKAGE_PREFIX = "com.emm."

        /** The graph currently exposes 30 injected Clock/TimeZone fields across :domain/:data/:presentation. */
        const val MIN_EXPECTED_TIME_FIELDS = 20

        val EXPECTED_VIEW_MODELS = sortedSetOf(
            "AccountsViewModel",
            "AddAccountViewModel",
            "AddCategoryViewModel",
            "AddEditLoanViewModel",
            "AddEditRecurringMovementViewModel",
            "AddTransactionViewModel",
            "AuthViewModel",
            "CategoriesViewModel",
            "EditTransactionViewModel",
            "HomeViewModel",
            "LoansViewModel",
            "PersonLoansViewModel",
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
            PersonLoansViewModel::class to { parametersOf("test-person-key") },
            // Nullable loan id; a non-null value exercises the edit branch over the create one.
            AddEditLoanViewModel::class to { parametersOf("test-loan-id") },
        )
    }
}
