package com.emm.retrofit.experiences.readassets.domain

import com.emm.retrofit.core.FlowResult
import com.emm.retrofit.core.asResult

class ExperiencesReader(private val repository: ExperiencesRepository) {

    fun read(): FlowResult<List<Experience>> {
        return repository.readExperiences().asResult()
    }
}