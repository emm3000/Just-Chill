package com.emm.justchill.experiences.hh.domain.transaction

import com.emm.justchill.Transactions
import com.emm.justchill.core.Result
import com.emm.justchill.core.asResult
import kotlinx.coroutines.flow.Flow

class TransactionLoaderByDateRange(private val repository: TransactionRepository) {

    fun load(
        startDateMillis: StartDateMillis,
        endDateMillis: EndDateMillis,
    ): Flow<Result<List<Transactions>>> {

        val validateInvalid = startDateMillis.value == 0L && endDateMillis.value == 0L
        if (validateInvalid) return repository.all().asResult()

        val callRepo: Flow<List<Transactions>> = repository.retrieveByDateRange(
            startDateMillis = startDateMillis.value,
            endDateMillis = endDateMillis.value
        )
        return callRepo.asResult()
    }
}

@JvmInline
value class StartDateMillis(val value: Long)

@JvmInline
value class EndDateMillis(val value: Long)