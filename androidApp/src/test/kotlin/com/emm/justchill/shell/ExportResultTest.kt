package com.emm.justchill.shell

import com.emm.justchill.core.ui.atoms.EmmSnackbarTone
import com.emm.justchill.feature.profile.ProfileMessage
import com.emm.justchill.feature.profile.toText
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.concurrent.CountDownLatch
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExportResultTest {

    private val deleted: MutableList<String> = mutableListOf()
    private val written: MutableList<String> = mutableListOf()
    private val notified: MutableList<Pair<String, EmmSnackbarTone>> = mutableListOf()

    private suspend fun settle(document: String?, pending: PendingExport?, io: CoroutineDispatcher) {
        settleExportResult(
            document = document,
            pending = pending,
            io = io,
            write = { target, _ -> written.add(target) },
            delete = { target -> deleted += target },
            notify = { message, tone -> notified += message to tone },
        )
    }

    @Test
    fun `a document that arrives with no pending payload is deleted and reported as an Error`() = runTest {
        settle(document = "orphan-document", pending = null, io = StandardTestDispatcher(testScheduler))

        assertEquals(listOf("orphan-document"), deleted)
        assertEquals(listOf(ProfileMessage.ExportFailed.toText() to EmmSnackbarTone.Error), notified)
        assertTrue(written.isEmpty())
    }

    @Test
    fun `the write runs on the io dispatcher and the result returns to the caller`() = runTest {
        val writeThreads: MutableList<String> = mutableListOf()
        val resultThreads: MutableList<String> = mutableListOf()
        val callerThread: String = Thread.currentThread().name

        ioProbe().use { io ->
            settleExportResult(
                document = "export-document",
                pending = PendingExport(json = "{}", onResult = { resultThreads += Thread.currentThread().name }),
                io = io,
                write = { _, _ -> writeThreads.add(Thread.currentThread().name) },
                delete = { },
                notify = { _, _ -> },
            )
        }

        assertEquals(listOf(IO_PROBE_THREAD), writeThreads)
        assertEquals(listOf(callerThread), resultThreads)
    }

    @Test
    fun `a host torn down mid-write still reports the saved export`() = runTest {
        val results: MutableList<Boolean> = mutableListOf()
        val writeStarted: CompletableDeferred<Unit> = CompletableDeferred()
        val writeReleased: CountDownLatch = CountDownLatch(1)

        ioProbe().use { io ->
            val host: Job = launch {
                settleExportResult(
                    document = "export-document",
                    pending = PendingExport(json = "{}", onResult = { results += it }),
                    io = io,
                    write = { _, _ ->
                        writeStarted.complete(Unit)
                        writeReleased.await()
                        true
                    },
                    delete = { },
                    notify = { _, _ -> },
                )
            }
            writeStarted.await()
            host.cancel()
            writeReleased.countDown()
            host.join()
        }

        assertEquals(listOf(true), results)
    }

    @Test
    fun `a cancelled picker with no pending payload stays silent`() = runTest {
        settle(document = null, pending = null, io = StandardTestDispatcher(testScheduler))

        assertTrue(deleted.isEmpty())
        assertTrue(notified.isEmpty())
        assertTrue(written.isEmpty())
    }

    @Test
    fun `a document with a pending payload is written and reported, never deleted`() = runTest {
        val results: MutableList<Boolean> = mutableListOf()

        settle(
            document = "export-document",
            pending = PendingExport(json = "{}", onResult = { results += it }),
            io = StandardTestDispatcher(testScheduler),
        )

        assertEquals(listOf("export-document"), written)
        assertEquals(listOf(true), results)
        assertTrue(deleted.isEmpty())
        assertTrue(notified.isEmpty())
    }
}
