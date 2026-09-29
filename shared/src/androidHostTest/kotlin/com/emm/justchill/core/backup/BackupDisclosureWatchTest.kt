package com.emm.justchill.core.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class BackupDisclosureWatchTest {

    private val isPending: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val delivered: MutableList<Boolean> = mutableListOf()

    @Test
    fun `every change of the pending disclosure reaches the closure`() = runTest {
        val watch = BackupDisclosureWatch(isPending, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        watch.start { pending: Boolean -> delivered += pending }
        isPending.value = true
        isPending.value = false
        watch.stop()

        assertEquals(listOf(false, true, false), delivered)
    }

    @Test
    fun `a stopped watch delivers nothing more`() = runTest {
        val watch = BackupDisclosureWatch(isPending, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        watch.start { pending: Boolean -> delivered += pending }
        watch.stop()
        isPending.value = true

        assertEquals(listOf(false), delivered)
    }
}
