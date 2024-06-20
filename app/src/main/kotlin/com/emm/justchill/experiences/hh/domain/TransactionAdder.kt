package com.emm.justchill.experiences.hh.domain

import com.emm.justchill.experiences.hh.data.TransactionInsert

class TransactionAdder(private val repository: TransactionRepository) {

    suspend fun add(param: TransactionInsert) {
        repository.add(param)
    }
}