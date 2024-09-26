package com.emm.justchill.loans

import com.emm.justchill.hh.data.shared.DefaultUniqueIdProvider
import com.emm.justchill.loans.data.DefaultLoanRepository
import com.emm.justchill.loans.data.DefaultPaymentRepository
import com.emm.justchill.loans.domain.LoanAndPaymentsCreator
import com.emm.justchill.loans.domain.LoanCreator
import com.emm.justchill.loans.domain.LoanRepository
import com.emm.justchill.loans.domain.PaymentRepository
import com.emm.justchill.loans.domain.PaymentsCreator
import com.emm.justchill.loans.domain.PaymentsGenerator
import com.emm.justchill.loans.presentation.LoansViewModel
import com.emm.justchill.quota.AddQuotaViewModel
import com.emm.justchill.quota.DriversViewModel
import com.emm.justchill.quota.QuotasViewModel
import com.emm.justchill.quota.data.LocalDriverRepository
import com.emm.justchill.quota.data.LocalQuotaRepository
import com.emm.justchill.quota.domain.DriverRepository
import com.emm.justchill.quota.domain.QuotaRepository
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.androidx.viewmodel.dsl.viewModelOf
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.bind
import org.koin.dsl.module

val loansModule = module {

    viewModelOf(::LoansViewModel)

    factory { DefaultPaymentRepository(get()) } bind PaymentRepository::class
    factory { DefaultLoanRepository(get()) } bind LoanRepository::class

    factory { LoanCreator(get(), get()) }
    factory { PaymentsCreator(get()) }
    factory { PaymentsGenerator(uniqueIdProvider = DefaultUniqueIdProvider) }

    factoryOf(::LoanAndPaymentsCreator)

    factoryOf(::LocalQuotaRepository) bind QuotaRepository::class
    factoryOf(::LocalDriverRepository) bind DriverRepository::class

    viewModelOf(::DriversViewModel)

    viewModel { parameters ->
        AddQuotaViewModel(
            driverRepository = get(),
            driverId = parameters.get(),
            quotaRepository = get(),
            dateAndTimeCombiner = get(),
        )
    }

    viewModel { parameters ->
        QuotasViewModel(
            driverRepository = get(),
            driverId = parameters.get(),
            quotaRepository = get(),
        )
    }
}