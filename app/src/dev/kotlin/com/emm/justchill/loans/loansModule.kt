package com.emm.justchill.loans

import com.emm.justchill.loans.data.LocalLoanRepository
import com.emm.justchill.loans.data.LocalPaymentRepository
import com.emm.justchill.loans.domain.LoanAndPaymentsCreator
import com.emm.justchill.loans.domain.LoanCreator
import com.emm.justchill.loans.domain.LoanRepository
import com.emm.justchill.loans.domain.PaymentRepository
import com.emm.justchill.loans.domain.PaymentsCreator
import com.emm.justchill.loans.domain.PaymentsGenerator
import com.emm.justchill.loans.presentation.AddLoanViewModel
import com.emm.justchill.loans.presentation.LoansViewModel
import com.emm.justchill.loans.presentation.PaymentsViewModel
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

    viewModel { parameters ->
        AddLoanViewModel(
            driverRepository = get(),
            driverId = parameters.get(),
            loanAndPaymentsCreator = get(),
        )
    }

    viewModel { parameters ->
        LoansViewModel(
            driverId = parameters.get(),
            loanRepository = get()
        )
    }

    viewModel { parameters ->
        PaymentsViewModel(
            loanId = parameters.get(),
            paymentRepository = get()
        )
    }

    factory { LocalPaymentRepository(get()) } bind PaymentRepository::class
    factory { LocalLoanRepository(get()) } bind LoanRepository::class

    factory { LoanCreator(get(), get()) }
    factory { PaymentsCreator(get()) }
    factory { PaymentsGenerator(get()) }

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