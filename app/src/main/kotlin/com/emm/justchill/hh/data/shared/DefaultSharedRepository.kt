package com.emm.justchill.hh.data.shared

import com.emm.justchill.hh.domain.shared.SharedRepository

class DefaultSharedRepository(
    private val sharedSqlDataSource: SharedSqlDataSource,
): SharedRepository {

    override suspend fun existData(): Boolean {
        return sharedSqlDataSource.hasAnyDataInLocalDataBase()
    }
}