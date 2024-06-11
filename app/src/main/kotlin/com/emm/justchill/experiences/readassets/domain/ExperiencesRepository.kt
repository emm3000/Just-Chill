package com.emm.justchill.experiences.readassets.domain

import kotlinx.coroutines.flow.Flow

interface ExperiencesRepository {

    fun readExperiences(): Flow<List<Experience>>
}