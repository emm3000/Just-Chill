package com.emm.justchill.hh.di

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.AccountRemoteDataSource
import com.emm.data.auth.DefaultAuthRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.CategoryRemoteDataSource
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionUpdateRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.data.transaction.TransactionRemoteDataSource
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.AuthenticateUserUseCase
import com.emm.domain.auth.CreateUserUseCase
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.hh.account.AccountsViewModel
import com.emm.justchill.hh.account.AddAccountViewModel
import com.emm.justchill.hh.auth.LoginViewModel
import com.emm.justchill.hh.auth.SignUpViewModel
import com.emm.justchill.hh.category.AddCategoryViewModel
import com.emm.justchill.hh.category.SelectCategoryViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.seetransactions.SeeTransactionsViewModel
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.transaction.AddTransactionViewModel
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val hhModule = module {

    repositoriesProviders()
    factoryOf(::AuthenticateUserUseCase)
    factoryOf(::CreateUserUseCase)

    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    viewModelsProviders()
    dataSource()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModelOf(::AddTransactionViewModel)
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::LoginViewModel)
    viewModelOf(::SelectCategoryViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            transactionUpdater = get(),
            transactionFinder = get(),
            transactionDeleter = get(),
            accountRepository = get(),
            accountFinder = get(),
        )
    }

    viewModelOf(::AddCategoryViewModel)
    viewModelOf(::AddAccountViewModel)

    viewModelOf(::AccountsViewModel)

    viewModelOf(::SignUpViewModel)
}


private fun Module.dataSource() {
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::AccountLocalDataSource)

    factoryOf(::TransactionRemoteDataSource)
    factoryOf(::CategoryRemoteDataSource)
    factoryOf(::AccountRemoteDataSource)
}

private fun Module.repositoriesProviders() {

    factoryOf(::DefaultTransactionRepository) {
        bind<TransactionRepository>()
    }

    factoryOf(::DefaultTransactionUpdateRepository) bind TransactionUpdateRepository::class

    factoryOf(::DefaultAuthRepository) bind  AuthRepository::class
}