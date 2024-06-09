package com.emm.retrofit.experiences.readassets.data

import kotlinx.coroutines.flow.Flow

interface ExperiencesDataSource {

    fun readExperiences(): Flow<List<ExperiencesLocalModel>>
}