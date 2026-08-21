package com.emm.justchill

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * `Application.onCreate` runs on no JVM host test and `:androidApp` has no instrumented source set,
 * so the one call that starts the sweep is reachable only as source text.
 */
class LegacySessionSweepStartupTest {

    @Test
    fun `onCreate starts the legacy session sweep`() {
        val source = read("src/main/kotlin/com/emm/justchill/EmmApp.kt")
        val sweepCall = source.indexOf(SWEEP_CALL)
        assertTrue(
            sweepCall != -1,
            "EmmApp no longer calls $SWEEP_CALL. On an install that predates the Keystore the " +
                "cleartext refresh token survives every launch until the user opens Perfil.",
        )

        val sweepStarter = Regex("""private fun (\w+)\(""")
            .findAll(source.take(sweepCall))
            .lastOrNull()
            ?.let { "${it.groupValues[1]}(" }
            ?: SWEEP_CALL
        val onCreateBody = source.substringAfter("override fun onCreate()").substringBefore("private fun")

        assertTrue(
            onCreateBody.contains(sweepStarter),
            "onCreate no longer starts the legacy session sweep. On an install that predates the " +
                "Keystore the cleartext refresh token survives every launch until the user opens Perfil.",
        )
    }

    private fun read(path: String): String {
        val file = File(path)
        assertTrue(file.isFile, "$path is missing at ${file.absolutePath}.")
        return file.readText()
    }
}

private const val SWEEP_CALL = ".sweepLegacySession()"
