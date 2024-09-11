package com.emm.justchill.hh.domain.shared

interface SharedRepository {

    suspend fun existData(): Boolean
}