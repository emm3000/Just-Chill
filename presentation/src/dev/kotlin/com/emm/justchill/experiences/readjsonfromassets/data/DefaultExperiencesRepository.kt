package com.emm.justchill.experiences.readjsonfromassets.data

import com.emm.justchill.experiences.readjsonfromassets.domain.Experience
import com.emm.justchill.experiences.readjsonfromassets.domain.ExperiencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DefaultExperiencesRepository(
    private val dataSource: ExperiencesDataSource,
) : ExperiencesRepository {

    override fun readExperiences(): Flow<List<Experience>> {
        return dataSource.readExperiences()
            .map(List<ExperiencesLocalModel>::toDomain)
    }
}