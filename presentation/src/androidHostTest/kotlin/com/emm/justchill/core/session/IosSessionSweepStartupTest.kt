package com.emm.justchill.core.session

import org.junit.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * `initKoin` runs on no JVM host test and this module has no iOS test source set, so the one call
 * that empties the cleartext session store at launch is reachable only as source text.
 */
class IosSessionSweepStartupTest {

    @Test
    fun `initKoin moves the session into the Keychain before Koin starts`() {
        val body = read("src/iosMain/kotlin/com/emm/justchill/KoinIos.kt")
            .substringAfter("fun initKoin() {")
            .substringBefore("\n}")

        val move = body.indexOf(MOVE_CALL)
        assertTrue(
            move != -1,
            "initKoin no longer calls $MOVE_CALL. On an install that predates the Keychain the " +
                "cleartext refresh token survives every launch — and swapping the two arguments " +
                "inverts the move, copying every Keychain session back into the clear.",
        )

        val koinStart = body.indexOf("startKoin")
        assertTrue(
            move in 0 until koinStart,
            "initKoin no longer moves the session before startKoin, so the SessionManager single " +
                "can serve the Keychain's older session while the cleartext one is still the newer.",
        )
    }

    private fun read(path: String): String {
        val file = File(path)
        assertTrue(file.isFile, "$path is missing at ${file.absolutePath}.")
        return file.readText()
    }
}

// The arguments are asserted, not just the symbol: swapping them keeps every other assertion green
// and inverts the migration.
private const val MOVE_CALL = "moveSessionToSecureStore(legacySessionSettings(), keychainSessionSettings())"
