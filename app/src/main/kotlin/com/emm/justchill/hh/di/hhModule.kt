package com.emm.justchill.hh.di

import android.content.Context
import com.emm.domain.auth.UserAuthenticator
import com.emm.domain.auth.UserCreator
import com.emm.domain.shared.DateAndTimeCombiner
import com.emm.domain.shared.UniqueIdProvider
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionUpdateRepository
import com.emm.justchill.R
import com.emm.justchill.hh.account.AddAccountViewModel
import com.emm.justchill.hh.auth.LoginViewModel
import com.emm.justchill.hh.category.CategoryViewModel
import com.emm.justchill.hh.fasttransaction.FastTransactionViewModel
import com.emm.justchill.hh.home.HomeViewModel
import com.emm.justchill.hh.shared.DefaultUniqueIdProvider
import com.emm.justchill.hh.seetransactions.SeeTransactionsViewModel
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionUpdateRepository
import com.emm.justchill.hh.transaction.EditTransactionViewModel
import com.emm.justchill.hh.transaction.TransactionViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val hhModule = module {

    repositoriesProviders()
    factoryOf(::UserAuthenticator)
    factoryOf(::UserCreator)

    factory { DateAndTimeCombiner() }
    factory { DefaultUniqueIdProvider } bind UniqueIdProvider::class

    viewModelsProviders()
}

private fun Module.viewModelsProviders() {
    viewModelOf(::HomeViewModel)
    viewModel {
        TransactionViewModel(
            transactionCreator = get(),
            accountRepository = get()
        )
    }
    viewModelOf(::SeeTransactionsViewModel)
    viewModelOf(::LoginViewModel)

    viewModel { parameters ->
        EditTransactionViewModel(
            transactionId = parameters.get(),
            transactionUpdater = get(),
            transactionFinder = get(),
            transactionDeleter = get(),
            accountFinder = get(),
            accountRepository = get()
        )
    }

    viewModelOf(::CategoryViewModel)
    viewModelOf(::AddAccountViewModel)

    viewModelOf(::FastTransactionViewModel)
}

private fun Module.repositoriesProviders() {

    single<TransactionRepository> {
        DefaultTransactionRepository(
            transactionsQueries = get(),
        )
    }

    single<SupabaseClient> { supabase(androidApplication()) }

    factory<TransactionUpdateRepository> {
        DefaultTransactionUpdateRepository(
            transactionQueries = get(),
        )
    }
}

private fun supabase(context: Context): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = context.getString(R.string.supabase_url),
        supabaseKey = context.getString(R.string.supabase_key)
    ) {
        install(Auth) {
            this.alwaysAutoRefresh = true
        }
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(Json { ignoreUnknownKeys = true })
    }
}