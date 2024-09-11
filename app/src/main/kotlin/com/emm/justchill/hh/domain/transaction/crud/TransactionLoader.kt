package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.Transactions
import com.emm.justchill.hh.domain.transaction.TransactionRepository
import kotlinx.coroutines.flow.Flow

class TransactionLoader(private val repository: TransactionRepository) {

    fun load(): Flow<List<Transactions>> {
        return repository.all()
    }
}