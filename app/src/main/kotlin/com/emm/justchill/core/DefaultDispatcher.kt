package com.emm.justchill.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class DefaultDispatcher : DispatchersProvider {

    override val mainDispatcher: CoroutineDispatcher
        get() = Dispatchers.Main

    override val ioDispatcher: CoroutineDispatcher
        get() = Dispatchers.IO

    override val defaultDispatcher: CoroutineDispatcher
        get() = Dispatchers.Default
}
