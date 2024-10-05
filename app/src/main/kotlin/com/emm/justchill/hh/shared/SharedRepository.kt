package com.emm.justchill.hh.shared

interface SharedRepository {

    suspend fun existData(): Boolean
}