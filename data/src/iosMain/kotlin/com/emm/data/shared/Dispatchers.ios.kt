package com.emm.data.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Kotlin/Native has no Dispatchers.IO; Default is the substitute.
 */
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
