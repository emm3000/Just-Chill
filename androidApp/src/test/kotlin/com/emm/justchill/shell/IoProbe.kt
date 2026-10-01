package com.emm.justchill.shell

import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

internal const val IO_PROBE_THREAD: String = "io-probe"

internal fun ioProbe(): ExecutorCoroutineDispatcher =
    Executors.newSingleThreadExecutor { task -> Thread(task, IO_PROBE_THREAD) }.asCoroutineDispatcher()
