package com.emm.justchill.core

import kotlinx.coroutines.CoroutineDispatcher

interface DispatchersProvider {

    val mainDispatcher: CoroutineDispatcher

    val ioDispatcher: CoroutineDispatcher

    val defaultDispatcher: CoroutineDispatcher
}
