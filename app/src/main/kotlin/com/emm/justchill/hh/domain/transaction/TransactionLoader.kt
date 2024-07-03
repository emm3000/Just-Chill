package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.core.Result
import com.emm.justchill.core.asResult
import kotlinx.coroutines.flow.Flow

class TransactionLoader(private val repository: TransactionRepository) {

    fun load(): Flow<Result<List<Transactions>>> {
        return repository.all().asResult()
    }
}