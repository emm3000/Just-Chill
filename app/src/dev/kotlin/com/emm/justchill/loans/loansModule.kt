package com.emm.justchill.loans

import com.emm.justchill.hh.domain.transactioncategory.DateAndTimeCombiner
import com.emm.justchill.loans.data.DefaultLoanRepository
import com.emm.justchill.loans.data.DefaultPaymentRepository
import com.emm.justchill.loans.domain.LoanAndPaymentsCreator
import com.emm.justchill.loans.domain.LoanCreator
import com.emm.justchill.loans.domain.PaymentsCreator
import com.emm.justchill.loans.domain.PaymentsGenerator
import com.emm.justchill.loans.presentation.LoansViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.dsl.module

val loansModule = module {

    viewModelOf(::LoansViewModel)

    factory {
        LoanAndPaymentsCreator(
            loanCreator = LoanCreator(
                repository = DefaultLoanRepository(),
                dateAndTimeCombiner = DateAndTimeCombiner()
            ),
            paymentsCreator = PaymentsCreator(repository = DefaultPaymentRepository()),
            paymentsGenerator = PaymentsGenerator(),
        )
    }
}