package com.emm.justchill.hh.data.category

interface CategorySaver {

    suspend fun save(name: String, type: String)
}