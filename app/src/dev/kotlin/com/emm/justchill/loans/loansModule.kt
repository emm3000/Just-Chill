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
import com.emm.justchill.daily.AddDailyViewModel
import com.emm.justchill.daily.DriversViewModel
import com.emm.justchill.daily.DailiesViewModel
import com.emm.justchill.daily.data.LocalDriverRepository
import com.emm.justchill.daily.data.LocalDailyRepository
import com.emm.justchill.daily.domain.DriverRepository
import com.emm.justchill.daily.domain.DailyRepository
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

    factoryOf(::LocalDailyRepository) bind DailyRepository::class
    factoryOf(::LocalDriverRepository) bind DriverRepository::class

    viewModelOf(::DriversViewModel)

    viewModel { parameters ->
        AddDailyViewModel(
            driverRepository = get(),
            driverId = parameters.get(),
            dailyRepository = get(),
            dateAndTimeCombiner = get(),
        )
    }

    viewModel { parameters ->
        DailiesViewModel(
            driverRepository = get(),
            driverId = parameters.get(),
            dailyRepository = get(),
        )
    }
}