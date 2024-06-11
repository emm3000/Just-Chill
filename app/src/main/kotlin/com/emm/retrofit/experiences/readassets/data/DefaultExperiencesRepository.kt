package com.emm.retrofit.experiences.readassets.data

import com.emm.retrofit.experiences.readassets.domain.Experience
import com.emm.retrofit.experiences.readassets.domain.ExperiencesRepository
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