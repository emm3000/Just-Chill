package com.emm.justchill.hh.di

import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.justchill.hh.loan.LoansViewModel
import com.emm.justchill.hh.loan.PersonLoansViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// Only DeleteLoanUseCase is bound: CreateLoanUseCase, UpdateLoanUseCase and
// RegisterLoanPaymentUseCase have no consumer yet (E05-08/E05-09).
val loanModule = module {
    factoryOf(::DeleteLoanUseCase)

    viewModelOf(::LoansViewModel)

    viewModel { parameters ->
        PersonLoansViewModel(
            personKey = parameters.get(),
            loanRepository = get(),
            deleteLoan = get(),
        )
    }
}
