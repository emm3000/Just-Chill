package com.emm.retrofit.experiences.readassets.domain

import kotlinx.coroutines.flow.Flow

interface ExperiencesRepository {

    fun readExperiences(): Flow<List<Experience>>
}