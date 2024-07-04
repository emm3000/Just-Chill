package com.emm.justchill.hh.domain

import android.os.Environment
import com.emm.justchill.BuildConfig
import com.emm.justchill.Categories
import com.emm.justchill.Transactions
import com.emm.justchill.TransactionsCategories
import com.emm.justchill.hh.domain.category.CategoryRepository
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transactioncategory.TransactionCategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combineTransform
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.IOException

class BackupManager(
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryTransactionRepository: TransactionCategoryRepository,
) {

    private val json by lazy {
        Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }
    }

    fun backup(): Flow<Boolean> {
        val categoriesFlow: Flow<List<Categories>> = categoryRepository.all()
        val transactionsFlow: Flow<List<Transactions>> = transactionRepository.all()
        val categoryTransactionsFlow: Flow<List<TransactionsCategories>> = categoryTransactionRepository.retrieve()

        return combineTransform(
            categoriesFlow, transactionsFlow, categoryTransactionsFlow
        ) { categories, transactions, categoryTransaction ->

            val categoriesModel: List<CategoryModel> = categories.map(Categories::toModel)
            val transactionsModel: List<TransactionModel> = transactions.map(Transactions::toModel)
            val categoryTransactionModel: List<TransactionCategoryModel> =
                categoryTransaction.map(TransactionsCategories::toModel)

            val isAllOk: Boolean = listOf(
                createJsonFile("categoriesModel", json.encodeToString(categoriesModel)),
                createJsonFile("transactionsModel", json.encodeToString(transactionsModel)),
                createJsonFile(
                    "categoryTransactionModel",
                    json.encodeToString(categoryTransactionModel)
                ),
            ).all { it }

            emit(isAllOk)
        }.catch {
            emit(false)
        }
    }

    private fun createJsonFile(filename: String, jsonContent: String): Boolean {
        if (Environment.getExternalStorageState() != Environment.MEDIA_MOUNTED) {
            return false
        }

        val externalStoragePublicDirectory: File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!externalStoragePublicDirectory.exists()) {
            externalStoragePublicDirectory.mkdirs()
        }

        val applicationId = BuildConfig.FLAVOR
        val jsonFile = File(externalStoragePublicDirectory, "$filename-$applicationId.json")

        try {
            FileWriter(jsonFile).use { writer ->
                writer.write(jsonContent)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    @Suppress("unused")
    fun readJsonFile(filename: String): String? {
        val externalStoragePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val jsonFile = File(externalStoragePublicDirectory, "$filename.json")

        return if (jsonFile.exists()) {
            try {
                FileInputStream(jsonFile).use { inputStream ->
                    inputStream.bufferedReader().use { reader ->
                        reader.readText()
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}