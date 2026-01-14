package com.emm.justchill.hh.di

import com.emm.data.category.DefaultCategoryRepository
import com.emm.domain.category.CategoryCreator
import com.emm.domain.category.CategoryDeleter
import com.emm.domain.category.CategoryFinder
import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpdater
import com.emm.justchill.hh.category.CategorySeeder
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

val categoryModule = module {

    factoryOf(::CategoryCreator)
    factoryOf(::CategoryDeleter)
    factoryOf(::CategoryUpdater)
    factoryOf(::CategoryFinder)

    factoryOf(::CategorySeeder)

    factoryOf(::DefaultCategoryRepository) {
        bind<CategoryRepository>()
    }
}