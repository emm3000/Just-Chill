package com.emm.justchill.core.backup

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class BackupDisclosureWatch(private val isPending: Flow<Boolean>, private val scope: CoroutineScope) {

    fun start(onPendingChange: (Boolean) -> Unit) {
        scope.launch { isPending.collect(onPendingChange) }
    }

    fun stop() {
        scope.cancel()
    }
}
