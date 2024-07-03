package com.emm.justchill.experiences.readjsonfromassets.data

import kotlinx.coroutines.flow.Flow

interface ExperiencesDataSource {

    fun readExperiences(): Flow<List<ExperiencesLocalModel>>
}