package com.emm.justchill.experiences.hh.data

interface CategorySaver {

    suspend fun save(name: String, type: String)
}