package com.emm.justchill.hh.di

import com.emm.domain.loan.CreateLoanUseCase
import com.emm.domain.loan.DeleteLoanUseCase
import com.emm.domain.loan.RegisterLoanPaymentUseCase
import com.emm.domain.loan.UpdateLoanPaymentUseCase
import com.emm.domain.loan.UpdateLoanUseCase
import com.emm.justchill.hh.loan.AddEditLoanViewModel
import com.emm.justchill.hh.loan.LoanDetailViewModel
import com.emm.justchill.hh.loan.LoansViewModel
import com.emm.justchill.hh.loan.PersonLoansViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val loanModule = module {
    factoryOf(::DeleteLoanUseCase)
    factoryOf(::CreateLoanUseCase)
    factoryOf(::UpdateLoanUseCase)
    factoryOf(::RegisterLoanPaymentUseCase)
    factoryOf(::UpdateLoanPaymentUseCase)

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
            clock = get(),
            zone = get(),
        )
    }
}
