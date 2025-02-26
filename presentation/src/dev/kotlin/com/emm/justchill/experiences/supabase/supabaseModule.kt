package com.emm.justchill.experiences.supabase

import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val supabaseModule = module {

    factoryOf(::SupabasePlayground)
}