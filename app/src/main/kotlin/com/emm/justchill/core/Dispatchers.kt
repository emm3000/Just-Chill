package com.emm.justchill.core

import kotlinx.coroutines.CoroutineDispatcher

interface Dispatchers {

    val mainDispatcher: CoroutineDispatcher

    val ioDispatcher: CoroutineDispatcher

    val defaultDispatcher: CoroutineDispatcher
}

class DefaultDispatcher : Dispatchers {

    override val mainDispatcher: CoroutineDispatcher
        get() = kotlinx.coroutines.Dispatchers.Main

    override val ioDispatcher: CoroutineDispatcher
        get() = kotlinx.coroutines.Dispatchers.IO

    override val defaultDispatcher: CoroutineDispatcher
        get() = kotlinx.coroutines.Dispatchers.Default
}