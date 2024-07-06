package com.emm.justchill.hh.domain

import com.emm.justchill.Categories
import com.emm.justchill.Transactions
import com.emm.justchill.TransactionsCategories
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOn

class SupabaseBackupManager(
    private val supabaseClient: SupabaseClient,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryTransactionRepository: TransactionCategoryRepository,
    private val transactionModelFactory: TransactionModelFactory,
    private val sharedRepository: SharedRepository,
) : BackupManager {

    override suspend fun backup(): Flow<Boolean> {

        val categoriesFlow: Flow<List<Categories>> = categoryRepository.all()
        val transactionsFlow: Flow<List<Transactions>> = transactionRepository.all()
        val categoryTransactionsFlow: Flow<List<TransactionsCategories>> = categoryTransactionRepository.retrieve()

        return combineTransform(
            flow = categoriesFlow,
            flow2 = transactionsFlow,
            flow3 = categoryTransactionsFlow,
        ) { categories, transactions, categoryTransaction ->

            val categoriesModel: List<CategoryModel> = categories
                .map(Categories::toModel)
                .map { it.copy(deviceId = transactionModelFactory.androidId()) }

            val transactionsModel: List<TransactionModel> = transactions
                .map(Transactions::toModel)
                .map { it.copy(deviceId = transactionModelFactory.androidId()) }

            val categoryTransactionModel: List<TransactionCategoryModel> = categoryTransaction
                .map(TransactionsCategories::toModel)
                .map { it.copy(deviceId = transactionModelFactory.androidId()) }

            supabaseClient.from("transaction").upsert(transactionsModel)
            supabaseClient.from("categories").upsert(categoriesModel)
            supabaseClient.from("transactionsCategories").upsert(categoryTransactionModel)

            emit(true)
        }.catch {
            FirebaseCrashlytics.getInstance().recordException(it)
            emit(false)
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun seed(): Boolean {
        if (sharedRepository.existData()) {
            return false
        }

        val deviceId = transactionModelFactory.androidId()

        val transactions: List<TransactionModel> = supabaseClient
            .from("transaction")
            .select {
                filter {
                    TransactionModel::deviceId eq deviceId
                }
            }
            .decodeList<TransactionModel>()

        val categories: List<CategoryModel> = supabaseClient
            .from("categories")
            .select {
                filter {
                    CategoryModel::deviceId eq deviceId
                }
            }
            .decodeList<CategoryModel>()

        val transactionCategories: List<TransactionCategoryModel> = supabaseClient
            .from("transactionsCategories")
            .select {
                filter {
                    TransactionCategoryModel::deviceId eq deviceId
                }
            }
            .decodeList<TransactionCategoryModel>()

        categoryRepository.seed(categories)
        transactionRepository.seed(transactions)
        categoryTransactionRepository.seed(transactionCategories)

        return true
    }
}