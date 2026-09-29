package com.emm.justchill.wiring

import com.emm.justchill.core.domain.loan.CreateLoanUseCase
import com.emm.justchill.core.domain.loan.DeleteLoanUseCase
import com.emm.justchill.core.domain.loan.RegisterLoanPaymentUseCase
import com.emm.justchill.core.domain.loan.UpdateLoanPaymentUseCase
import com.emm.justchill.core.domain.loan.UpdateLoanUseCase
import com.emm.justchill.feature.loan.loanModule
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val loanWiring: Module = module {
    includes(loanModule)

    factoryOf(::DeleteLoanUseCase)
    factoryOf(::CreateLoanUseCase)
    factoryOf(::UpdateLoanUseCase)
    factoryOf(::RegisterLoanPaymentUseCase)
    factoryOf(::UpdateLoanPaymentUseCase)
}
