package com.emm.justchill.core

import android.content.Context
import androidx.lifecycle.ViewModel
import com.emm.justchill.core.backup.BackupController
import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.feature.category.AddCategoryViewModel
import com.emm.justchill.feature.loan.AddEditLoanViewModel
import com.emm.justchill.feature.loan.LoanDetailViewModel
import com.emm.justchill.feature.loan.PersonLoansViewModel
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

// A missing, mistyped or wrongly qualified binding in appModules() compiles cleanly AND survives
// assembleDevDebug, surfacing only as a crash when a user navigates to the affected screen. This
// test resolves EVERY definition to close that gap; bootstrapAppGraph itself is never called.
@OptIn(KoinInternalApi::class)
class AppGraphKoinTest {

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        // supabaseModule's install(Auth) needs a Context from an androidx.startup Initializer that
        // only runs inside a real app; SettingsInitializer.create is the documented hook to supply
        // one from tests. A relaxed mock is enough — nothing here reads or writes preferences.
        SettingsInitializer().create(mockk<Context>(relaxed = true))

        // Standard (not Unconfined): init-block coroutines stay queued and never run, so this test
        // measures WIRING only and cannot flake on database contents or network reachability.
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

        // Exhaustive by design: a ViewModel missing from this list resolves to nothing at
        // navigation time, so a new one must be added both here and to its Koin module.
        assertEquals(
            EXPECTED_VIEW_MODELS,
            registered.toSortedSet(),
            "The set of ViewModels registered in appModules() changed.",
        )

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

    // The sweep above proves every BOUND definition resolves; it cannot notice one that was never
    // bound. BackupOrchestrator is resolved behind a kill switch that is off today, so a deleted
    // binding would compile and keep the sweep green. No assertion body: get() throws on failure.
    @Test
    fun `every single bootstrapAppGraph resolves is bound`() {
        koin.get<BackupOrchestrator>()
    }

    // backupModule publishes BackupController as a secondary type of the orchestrator's single. A
    // second definition or a factory would still pass every other test while silently breaking a
    // manual backup (ADR 009 hard constraint 4) — assertSame catches what assertEquals cannot.
    @Test
    fun `the backup controller port is the orchestrator single, not a second instance`() {
        assertSame(koin.get<BackupOrchestrator>(), koin.get<BackupController>())
    }

    // Resolution succeeding is not the same as resolution being correct: sharedModule binds
    // Clock.System, indistinguishable from a hand-written block's default (ProfileModule once
    // forgot clock = get() and nothing noticed). Sentinel instances plus assertSame catch that.
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

    // Third-party singles are skipped: Supabase and Ktor carry clocks of their own that this
    // project does not bind and has no business asserting on.
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

    // Each definition's primary type plus every interface it is bind-ed to, since consumers inject
    // the interface.
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

    // Koin wraps every resolution failure in one InstanceCreationException per nesting level; the
    // innermost cause is the actionable one — it names the type that has no definition.
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

        // A floor, deliberately under the real count: adding an injected date field must not fail this.
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
            "LoanDetailViewModel",
            "LoansViewModel",
            "PersonLoansViewModel",
            "ProfileViewModel",
            "RecurringMovementsViewModel",
            "ReportViewModel",
            "SeeTransactionsViewModel",
        )

        // Each entry is a factory, not a shared holder, so one resolution cannot consume the
        // parameters of another.
        val RUNTIME_PARAMETERS: Map<KClass<*>, ParametersDefinition> = mapOf(
            AddCategoryViewModel::class to { parametersOf(CategoryType.Spend, "Test Category") },
            EditTransactionViewModel::class to { parametersOf("test-transaction-id") },
            // Non-null so this exercises the edit branch over the create one.
            AddEditRecurringMovementViewModel::class to { parametersOf("test-recurring-id") },
            PersonLoansViewModel::class to { parametersOf("test-person-key") },
            LoanDetailViewModel::class to { parametersOf("test-loan-id") },
            AddEditLoanViewModel::class to { parametersOf("test-loan-id") },
        )
    }
}
