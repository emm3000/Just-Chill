package com.emm.justchill.experiences.hh.data.category

interface CategorySaver {

    suspend fun save(name: String, type: String)
}