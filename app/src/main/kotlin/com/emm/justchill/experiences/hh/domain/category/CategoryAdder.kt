package com.emm.justchill.experiences.hh.domain.category

class CategoryAdder(private val repository: CategoryRepository) {

    suspend fun add(name: String, type: String) {
        repository.add(name, type)
    }
}