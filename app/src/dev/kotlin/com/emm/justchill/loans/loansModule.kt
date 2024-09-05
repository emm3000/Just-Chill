package com.emm.justchill.loans

import com.emm.justchill.hh.data.DefaultUniqueIdProvider
import com.emm.justchill.hh.domain.DateAndTimeCombiner
import com.emm.justchill.loans.data.DefaultLoanRepository
import com.emm.justchill.loans.data.DefaultPaymentRepository
import com.emm.justchill.loans.domain.LoanAndPaymentsCreator
import com.emm.justchill.loans.domain.LoanCreator
import com.emm.justchill.loans.domain.LoanRepository
import com.emm.justchill.loans.domain.PaymentRepository
import com.emm.justchill.loans.domain.PaymentsCreator
import com.emm.justchill.loans.domain.PaymentsGenerator
import com.emm.justchill.loans.presentation.LoansViewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val loansModule = module {

    viewModelOf(::LoansViewModel)

    factory { DefaultPaymentRepository(get()) } bind PaymentRepository::class
    factory { DefaultLoanRepository(get()) } bind LoanRepository::class

    factory { DateAndTimeCombiner() }
    factory { LoanCreator(get(), get()) }
    factory { PaymentsCreator(get()) }
    factory { PaymentsGenerator(uniqueIdProvider = DefaultUniqueIdProvider) }

    factoryOf(::LoanAndPaymentsCreator)
}