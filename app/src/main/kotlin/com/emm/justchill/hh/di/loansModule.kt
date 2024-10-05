package com.emm.justchill.hh.di

import com.emm.justchill.hh.me.loan.data.LocalLoanRepository
import com.emm.justchill.hh.me.payment.data.LocalPaymentRepository
import com.emm.justchill.hh.me.loan.domain.LoanAndPaymentsCreator
import com.emm.justchill.hh.me.loan.domain.LoanCreator
import com.emm.justchill.hh.me.loan.domain.LoanRepository
import com.emm.justchill.hh.me.payment.domain.PaymentRepository
import com.emm.justchill.hh.me.payment.domain.PaymentsCreator
import com.emm.justchill.hh.me.payment.domain.PaymentsGenerator
import com.emm.justchill.hh.me.loan.presentation.AddLoanViewModel
import com.emm.justchill.hh.me.loan.presentation.LoansViewModel
import com.emm.justchill.hh.me.payment.presentation.PaymentsViewModel
import com.emm.justchill.hh.me.daily.presentation.AddDailyViewModel
import com.emm.justchill.hh.me.driver.presentation.DriversViewModel
import com.emm.justchill.hh.me.daily.presentation.DailiesViewModel
import com.emm.justchill.hh.me.driver.data.LocalDriverRepository
import com.emm.justchill.hh.me.daily.data.LocalDailyRepository
import com.emm.justchill.hh.me.driver.domain.DriverRepository
import com.emm.justchill.hh.me.daily.domain.DailyRepository
import com.emm.justchill.hh.me.export.DataExporter
import com.emm.justchill.hh.me.driver.presentation.DriverViewViewModel
import com.emm.justchill.hh.me.home.HomeViewModel
import org.koin.android.ext.koin.androidApplication
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

    factory {
        DataExporter(
            context = androidApplication(),
            driverRepository = get(),
            dailyRepository = get(),
            loanRepository = get(),
            paymentRepository = get()
        )
    }

    viewModelOf(::HomeViewModel)
    viewModel {
        DriverViewViewModel(
            driverId = it.get(),
            driverRepository = get(),
            loanRepository = get(),
            dailyRepository = get(),
            dateAndTimeCombiner = get(),
            transactionCreator = get(),
            dailyAccountCreator = get(),
        )
    }

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
            transactionCreator = get(),
            dailyAccountCreator = get(),
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