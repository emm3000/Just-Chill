package com.emm.justchill.hh.domain.transaction

import com.emm.justchill.hh.data.transaction.TransactionInsert

class TransactionAdder(private val repository: TransactionRepository) {

    suspend fun add(param: com.emm.justchill.hh.data.transaction.TransactionInsert) {
        repository.add(param)
    }
}