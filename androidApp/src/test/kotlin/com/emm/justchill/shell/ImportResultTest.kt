package com.emm.justchill.shell

import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.feature.profile.ProfileMessage
import com.emm.justchill.feature.profile.toText
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ImportResultTest {

    private val imported: MutableList<String> = mutableListOf()
    private val notified: MutableList<Pair<String, EmmSnackbarTone>> = mutableListOf()

    private suspend fun settle(document: String?, io: CoroutineDispatcher, read: (String) -> String?) {
        settleImportResult(
            document = document,
            io = io,
            read = read,
            onImport = { text -> imported += text },
            notify = { message, tone -> notified += message to tone },
        )
    }

    @Test
    fun `a document whose stream is null is reported as an Error and never imported`() = runTest {
        settle(document = "unreadable-document", io = StandardTestDispatcher(testScheduler), read = { null })

        assertEquals(listOf(ProfileMessage.ImportFailed.toText() to EmmSnackbarTone.Error), notified)
        assertTrue(imported.isEmpty())
    }

    @Test
    fun `a document whose read throws is reported as an Error and never imported`() = runTest {
        settle(
            document = "broken-document",
            io = StandardTestDispatcher(testScheduler),
            read = { throw IOException("provider gone") },
        )

        assertEquals(listOf(ProfileMessage.ImportFailed.toText() to EmmSnackbarTone.Error), notified)
        assertTrue(imported.isEmpty())
    }

    @Test
    fun `a readable document hands its text to the import and stays silent`() = runTest {
        settle(document = "backup-document", io = StandardTestDispatcher(testScheduler), read = { "{\"v\":4}" })

        assertEquals(listOf("{\"v\":4}"), imported)
        assertTrue(notified.isEmpty())
    }

    @Test
    fun `the read runs on the io dispatcher and the hand-off returns to the caller`() = runTest {
        val readThreads: MutableList<String> = mutableListOf()
        val importThreads: MutableList<String> = mutableListOf()
        val callerThread: String = Thread.currentThread().name

        Executors.newSingleThreadExecutor { task -> Thread(task, "io-probe") }.asCoroutineDispatcher().use { io ->
            settleImportResult(
                document = "backup-document",
                io = io,
                read = { "{}".also { readThreads += Thread.currentThread().name } },
                onImport = { importThreads += Thread.currentThread().name },
                notify = { _, _ -> },
            )
        }

        assertEquals(listOf("io-probe"), readThreads)
        assertEquals(listOf(callerThread), importThreads)
    }

    @Test
    fun `a cancelled picker stays silent`() = runTest {
        settle(document = null, io = StandardTestDispatcher(testScheduler), read = { "{}" })

        assertTrue(imported.isEmpty())
        assertTrue(notified.isEmpty())
    }
}
