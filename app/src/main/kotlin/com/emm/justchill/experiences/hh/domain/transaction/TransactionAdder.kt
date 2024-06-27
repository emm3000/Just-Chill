package com.emm.justchill.experiences.hh.domain.transaction

import com.emm.justchill.experiences.hh.data.transaction.TransactionInsert

class TransactionAdder(private val repository: TransactionRepository) {

    suspend fun add(param: TransactionInsert) {
        repository.add(param)
    }
}