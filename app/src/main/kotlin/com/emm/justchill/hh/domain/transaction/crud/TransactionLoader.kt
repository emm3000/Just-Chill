package com.emm.justchill.hh.domain.transaction.crud

import com.emm.justchill.hh.domain.transaction.TransactionRepository
import com.emm.justchill.hh.domain.transaction.model.Transaction
import kotlinx.coroutines.flow.Flow

class TransactionLoader(private val repository: TransactionRepository) {

    fun load(): Flow<List<Transaction>> {
        return repository.all()
    }
}