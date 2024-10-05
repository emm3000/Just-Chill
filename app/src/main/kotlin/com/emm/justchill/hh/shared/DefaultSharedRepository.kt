package com.emm.justchill.hh.shared

class DefaultSharedRepository(
    private val sharedSqlDataSource: SharedSqlDataSource,
): SharedRepository {

    override suspend fun existData(): Boolean {
        return sharedSqlDataSource.hasAnyDataInLocalDataBase()
    }
}