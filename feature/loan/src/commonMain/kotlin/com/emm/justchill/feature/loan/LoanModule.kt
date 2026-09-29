package com.emm.justchill.feature.loan

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loanModule: Module = module {
    viewModelOf(::LoansViewModel)

    viewModel { parameters ->
        PersonLoansViewModel(
            personKey = parameters.get(),
            loanRepository = get(),
        )
    }

    viewModel { parameters ->
        LoanDetailViewModel(
            loanId = parameters.get(),
            loanRepository = get(),
            loanPaymentRepository = get(),
            deleteLoan = get(),
            registerLoanPayment = get(),
            updateLoanPayment = get(),
            todayFlow = get(),
            clock = get(),
            zone = get(),
        )
    }

    viewModel { parameters ->
        AddEditLoanViewModel(
            loanId = parameters.getOrNull(),
            loanRepository = get(),
            createLoan = get(),
            updateLoan = get(),
            todayFlow = get(),
            clock = get(),
            zone = get(),
        )
    }
}
