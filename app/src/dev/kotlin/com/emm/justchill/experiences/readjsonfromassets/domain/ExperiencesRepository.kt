package com.emm.justchill.experiences.readjsonfromassets.domain

import kotlinx.coroutines.flow.Flow

interface ExperiencesRepository {

    fun readExperiences(): Flow<List<Experience>>
}