package com.emm.justchill.hh.domain

import android.content.Context
import com.emm.justchill.Categories
import com.emm.justchill.Transactions
import com.emm.justchill.TransactionsCategories
import com.emm.justchill.hh.domain.FilesManager.createJsonFile
import com.emm.justchill.hh.domain.FilesManager.readJsonFile
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DefaultBackupManager(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryTransactionRepository: TransactionCategoryRepository,
    private val sharedRepository: SharedRepository,
    private val context: Context,
) : BackupManager {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    override suspend fun backup(): Flow<Boolean> {
        if (sharedRepository.existData().not()) {
            return flowOf(false)
        }

        val categoriesFlow: Flow<List<Categories>> = categoryRepository.all()
        val transactionsFlow: Flow<List<Transactions>> = transactionRepository.all()
        val categoryTransactionsFlow: Flow<List<TransactionsCategories>> = categoryTransactionRepository.retrieve()

        return combineTransform(
            flow = categoriesFlow,
            flow2 = transactionsFlow,
            flow3 = categoryTransactionsFlow,
        ) { categories, transactions, categoryTransaction ->

            val categoriesModel: List<CategoryModel> = categories.map(Categories::toModel)
            val transactionsModel: List<TransactionModel> = transactions.map(Transactions::toModel)
            val categoryTransactionModel: List<TransactionCategoryModel> =
                categoryTransaction.map(TransactionsCategories::toModel)

            val isAllOk: Boolean = listOf(
                createJsonFile(
                    context,
                    filename = CATEGORIES_MODEL,
                    jsonContent = json.encodeToString(categoriesModel)
                ),
                createJsonFile(
                    context,
                    filename = TRANSACTIONS_MODEL,
                    jsonContent = json.encodeToString(transactionsModel)
                ),
                createJsonFile(
                    context,
                    filename = CATEGORY_TRANSACTION_MODEL,
                    jsonContent = json.encodeToString(categoryTransactionModel)
                ),
            ).all { it }

            emit(isAllOk)
        }.catch {
            emit(false)
        }
    }

    override suspend fun seed(): Boolean {
        return if (sharedRepository.existData().not()) {
            val categories = readJsonFile(context, CATEGORIES_MODEL).orEmpty()
            val transactions = readJsonFile(context, TRANSACTIONS_MODEL).orEmpty()
            val transactionsCategories = readJsonFile(context, CATEGORY_TRANSACTION_MODEL).orEmpty()

            val categoryModels: List<CategoryModel> = serialize<CategoryModel>(categories)
            val transactionModels = serialize<TransactionModel>(transactions)
            val transactionCategoryModels =
                serialize<TransactionCategoryModel>(transactionsCategories)
            try {
                categoryRepository.seed(categoryModels)
                transactionRepository.seed(transactionModels)
                categoryTransactionRepository.seed(transactionCategoryModels)
                true
            } catch (e: Throwable) {
                FirebaseCrashlytics.getInstance().recordException(e)
                false
            }
        } else {
            false
        }
    }

    private inline fun <reified T> serialize(data: String): List<T> = try {
        json.decodeFromString<List<T>>(data)
    } catch (e: Throwable) {
        FirebaseCrashlytics.getInstance().recordException(e)
        emptyList()
    }

    companion object {

        private const val CATEGORIES_MODEL = "categoriesModel"
        private const val TRANSACTIONS_MODEL = "transactionsModel"
        private const val CATEGORY_TRANSACTION_MODEL = "categoryTransactionModel"
    }
}