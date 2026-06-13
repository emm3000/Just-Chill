package com.emm.data.shared

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// iOS has no dedicated IO dispatcher. Default is the standard substitute.
// TODO(phase6): verify SQLDelight native-driver threading needs under load.
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default
