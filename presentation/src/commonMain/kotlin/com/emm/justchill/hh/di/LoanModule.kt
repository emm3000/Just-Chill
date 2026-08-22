package com.emm.justchill.hh.di

import com.emm.domain.loan.CreateLoanUseCase
import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.UpdateLoanUseCase
import com.emm.justchill.hh.loan.AddEditLoanViewModel
import com.emm.justchill.hh.loan.LoansViewModel
import com.emm.justchill.hh.loan.PersonLoansViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

// RegisterLoanPaymentUseCase has no consumer yet (E05-09).
val loanModule = module {
    factoryOf(::DeleteLoanUseCase)
    factoryOf(::CreateLoanUseCase)
    factoryOf(::UpdateLoanUseCase)

    viewModelOf(::LoansViewModel)

    viewModel { parameters ->
        PersonLoansViewModel(
            personKey = parameters.get(),
            loanRepository = get(),
            deleteLoan = get(),
        )
    }

    viewModel { parameters ->
        AddEditLoanViewModel(
            loanId = parameters.getOrNull(),
            loanRepository = get(),
            createLoan = get(),
            updateLoan = get(),
            clock = get(),
            zone = get(),
        )
    }
}
