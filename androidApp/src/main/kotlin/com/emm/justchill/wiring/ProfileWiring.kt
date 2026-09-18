package com.emm.justchill.wiring

import com.emm.justchill.feature.profile.profileModule
import org.koin.core.module.Module
import org.koin.dsl.module

val profileWiring: Module = module {
    includes(profileModule)
}
