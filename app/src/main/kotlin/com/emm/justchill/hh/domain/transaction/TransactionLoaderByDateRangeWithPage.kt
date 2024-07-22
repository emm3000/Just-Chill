package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Transactions
import kotlinx.coroutines.flow.Flow

class TransactionLoaderByDateRangeWithPage(private val repository: TransactionRepository) {

    fun load(page: Long, pageSize: Long): Flow<List<Transactions>> {

        val offset = (page - 1) * pageSize

        return repository.retrieveWithLimit(
            limit = pageSize,
            offset = offset,
        )
    }
}