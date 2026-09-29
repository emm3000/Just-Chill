package com.emm.justchill.core

import android.content.Context
import com.emm.justchill.core.di.kitModules
import com.emm.justchill.core.testing.MainDispatcherRule
import com.russhwolf.settings.SettingsInitializer
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.Koin
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.qualifier.Qualifier
import org.koin.dsl.koinApplication
import kotlin.reflect.KClass
import kotlin.test.assertTrue

@OptIn(KoinInternalApi::class)
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
        koin.close()
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
    }
}
