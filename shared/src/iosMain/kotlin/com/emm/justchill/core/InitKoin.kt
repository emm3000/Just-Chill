package com.emm.justchill.core

import com.emm.justchill.core.di.kitModules
import com.emm.justchill.wiring.featureWirings
import org.koin.core.context.startKoin

fun initKoin(config: KitConfig) {
    startKoin { modules(kitModules + featureWirings + iosPlatformModule(config)) }
}
