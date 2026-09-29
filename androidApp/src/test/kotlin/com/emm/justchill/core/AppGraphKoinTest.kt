package com.emm.justchill.core

import android.content.Context
import androidx.lifecycle.ViewModel
import com.emm.justchill.core.backup.BackupOrchestrator
import com.emm.justchill.core.domain.category.CategoryType
import com.emm.justchill.core.domain.shared.backup.BackupController
import com.emm.justchill.core.testing.MainDispatcherRule
import com.emm.justchill.feature.category.AddCategoryViewModel
import com.emm.justchill.feature.loan.AddEditLoanViewModel
import com.emm.justchill.feature.loan.LoanDetailViewModel
import com.emm.justchill.feature.loan.PersonLoansViewModel
import com.emm.justchill.feature.transaction.capture.EditTransactionViewModel
import com.russhwolf.settings.SettingsInitializer
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.datetime.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Rule
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

@OptIn(KoinInternalApi::class)
class AppGraphKoinTest {

    private lateinit var koin: Koin

    // Standard, not Unconfined: init-block coroutines stay queued, so the test never reads the
    // database or the network.
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    @Before
    fun setUp() {
        // supabaseModule's install(Auth) takes its Context from an androidx.startup Initializer that
        // runs only inside a real app; SettingsInitializer.create is the documented hook for tests.
        SettingsInitializer().create(mockk<Context>(relaxed = true))
        koin = koinApplication { modules(appModules(testPlatformModule)) }.koin
    }

    @After
    fun tearDown() {
        koin.close()
    }

    @Test
    fun `every definition in the app graph resolves`() {
        val boundTypes: List<Pair<KClass<*>, Qualifier?>> = koin.boundTypes()
        val failures: MutableList<String> = mutableListOf()

        for ((type, qualifier) in boundTypes) {
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

    @Test
    fun `every single bootstrapAppGraph resolves is bound`() {
        koin.get<BackupOrchestrator>()
    }

    @Test
    fun `the backup controller port is the orchestrator single, not a second instance`() {
        assertSame(koin.get<BackupOrchestrator>(), koin.get<BackupController>())
    }

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

    private fun Koin.boundTypes(): List<Pair<KClass<*>, Qualifier?>> = definitions()
        .flatMap { definition ->
            (listOf(definition.primaryType) + definition.secondaryTypes).map { it to definition.qualifier }
        }
        .distinct()

    private fun Koin.definitions(): List<BeanDefinition<*>> = instanceRegistry.instances.values
        .distinct()
        .map { it.beanDefinition }

    private fun runtimeParametersFor(type: KClass<*>): ParametersDefinition? = RUNTIME_PARAMETERS[type]

    private fun describe(type: KClass<*>, qualifier: Qualifier?): String =
        if (qualifier == null) type.simpleName.orEmpty() else "${type.simpleName}(${qualifier.value})"

    private fun Exception.rootCauseMessage(): String {
        var root: Throwable = this
        while (root.cause != null && root.cause !== root) {
            root = checkNotNull(root.cause)
        }
        return (root.message ?: root::class.simpleName.orEmpty()).lineSequence().first().trim()
    }

    private companion object {

        const val MIN_EXPECTED_BINDINGS: Int = 80

        const val OWN_PACKAGE_PREFIX: String = "com.emm."

        // A floor, deliberately under the real count: adding an injected date field must not fail this.
        const val MIN_EXPECTED_TIME_FIELDS: Int = 20

        val EXPECTED_VIEW_MODELS = sortedSetOf(
            "AccountsViewModel",
            "AddAccountViewModel",
            "AddCategoryViewModel",
            "AddEditLoanViewModel",
            "AddTransactionViewModel",
            "AuthViewModel",
            "CategoriesViewModel",
            "EditTransactionViewModel",
            "LoanDetailViewModel",
            "LoansViewModel",
            "PersonLoansViewModel",
            "ProfileViewModel",
            "ReportViewModel",
            "SeeTransactionsViewModel",
        )

        val RUNTIME_PARAMETERS: Map<KClass<*>, ParametersDefinition> = mapOf(
            AddCategoryViewModel::class to { parametersOf(CategoryType.Spend, "Test Category") },
            EditTransactionViewModel::class to { parametersOf("test-transaction-id") },
            PersonLoansViewModel::class to { parametersOf("test-person-key") },
            LoanDetailViewModel::class to { parametersOf("test-loan-id") },
            AddEditLoanViewModel::class to { parametersOf("test-loan-id") },
        )
    }
}
