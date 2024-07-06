package com.emm.justchill.hh.domain

interface SharedRepository {

    suspend fun existData(): Boolean
}