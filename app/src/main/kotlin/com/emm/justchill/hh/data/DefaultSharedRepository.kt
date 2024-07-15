package com.emm.justchill.hh.data

import com.emm.justchill.hh.domain.SharedRepository

class DefaultSharedRepository(
    private val sharedSqlDataSource: SharedSqlDataSource,
): SharedRepository {

    override suspend fun existData(): Boolean {
        return sharedSqlDataSource.hasAnyDataInLocalDataBase()
    }
}