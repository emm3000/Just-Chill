package com.emm.justchill.core

import com.emm.justchill.core.di.kitModules
import org.koin.core.context.startKoin

fun initKoin(config: KitConfig) {
    startKoin { modules(kitModules + iosPlatformModule(config)) }
}
