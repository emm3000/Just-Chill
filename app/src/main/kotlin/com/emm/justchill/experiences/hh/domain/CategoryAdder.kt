package com.emm.justchill.experiences.hh.domain

class CategoryAdder(private val repository: CategoryRepository) {

    suspend fun add(name: String, type: String) {
        repository.add(name, type)
    }
}