package com.emm.justchill.hh.data.transaction

import com.emm.justchill.EmmDatabase
import com.emm.justchill.TransactionQueries
import com.emm.justchill.TransactionsCategoriesQueries
import com.emm.justchill.hh.domain.transaction.TransactionUpdateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DefaultTransactionUpdateRepository(
    private val emmDatabase: EmmDatabase,
    private val transactionQueries: TransactionQueries,
    private val transactionCategoryQueries: TransactionsCategoriesQueries,
) : TransactionUpdateRepository {

    override suspend fun update(
        transactionId: String,
        transactionUpdate: TransactionUpdate,
        categoryId: String
    ) = withContext(Dispatchers.IO) {
        emmDatabase.transaction {
            transactionCategoryQueries.delete(transactionId)
            transactionQueries.updateValues(
                type = transactionUpdate.type,
                amount = transactionUpdate.amount,
                description = transactionUpdate.description,
                date = transactionUpdate.date,
                transactionId = transactionId
            )
            transactionCategoryQueries.add(
                transactionId = transactionId,
                categoryId = categoryId
            )
        }
    }
}