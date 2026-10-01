package com.emm.justchill.core

import android.content.Context
import com.emm.justchill.core.di.kitModules
import com.emm.justchill.core.testing.MainDispatcherRule
import com.russhwolf.settings.SettingsInitializer
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.annotations.SupabaseInternal
import io.github.jan.supabase.auth.auth
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import kotlin.coroutines.ContinuationInterceptor
import kotlin.reflect.KClass
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@OptIn(KoinInternalApi::class, SupabaseInternal::class)
class KitGraphKoinTest {

    @get:Rule
    val mainDispatcherRule: MainDispatcherRule = MainDispatcherRule(StandardTestDispatcher())

    private lateinit var koin: Koin

    @Before
    fun setUp() {
        SettingsInitializer().create(mockk<Context>(relaxed = true))
        koin = koinApplication { modules(kitModules + kitTestPlatformModule) }.koin
    }

    @After
    fun tearDown() {
        koin.closeGraph()
    }

    @Test
    fun `every kit definition resolves with nothing but a platform module beside it`() {
        val boundTypes: List<Pair<KClass<*>, Qualifier?>> = koin.boundTypes()

        val failures: List<String> = boundTypes.mapNotNull { (type, qualifier) ->
            runCatching { koin.get<Any>(type, qualifier) }
                .exceptionOrNull()
                ?.let { "${type.simpleName}(${qualifier?.value.orEmpty()}) -> ${it.rootCause().message}" }
        }

        assertTrue(failures.isEmpty(), "${failures.size} of ${boundTypes.size} failed:\n${failures.joinToString("\n")}")
        assertTrue(boundTypes.size >= MIN_EXPECTED_BINDINGS, "Only ${boundTypes.size} bindings were discovered.")
    }

    // supabase-kt's Auth startup runs on Dispatchers.Default and ends in a launch(Dispatchers.Main)
    // (setupPlatform); left running past the test, that read races MainDispatcherRule's next setMain (#630).
    private fun Koin.closeGraph() {
        val authJob: Job = get<SupabaseClient>().auth.authScope.coroutineContext.job
        authJob.cancel()
        runBlocking {
            withTimeout(AUTH_STARTUP_TIMEOUT) {
                var pending: List<Job> = authJob.offMainChildren()
                while (pending.isNotEmpty()) {
                    pending.joinAll()
                    pending = authJob.offMainChildren()
                }
            }
        }
        close()
    }

    private fun Job.offMainChildren(): List<Job> = children
        .filter { (it as? CoroutineScope)?.coroutineContext?.get(ContinuationInterceptor) !== Dispatchers.Main }
        .toList()

    private fun Koin.boundTypes(): List<Pair<KClass<*>, Qualifier?>> = instanceRegistry.instances.values
        .distinct()
        .map { it.beanDefinition }
        .flatMap { definition ->
            (listOf(definition.primaryType) + definition.secondaryTypes).map { it to definition.qualifier }
        }
        .distinct()

    private fun Throwable.rootCause(): Throwable = generateSequence(this) { it.cause }.last()

    private companion object {
        const val MIN_EXPECTED_BINDINGS: Int = 56

        val AUTH_STARTUP_TIMEOUT: Duration = 5.seconds
    }
}
