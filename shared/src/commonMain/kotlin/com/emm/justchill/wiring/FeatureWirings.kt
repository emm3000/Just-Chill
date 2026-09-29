package com.emm.justchill.wiring

import org.koin.core.module.Module

val featureWirings: List<Module> = listOf(
    transactionWiring,
    accountWiring,
    categoryWiring,
    reportWiring,
    loanWiring,
    profileWiring,
    authWiring,
)
