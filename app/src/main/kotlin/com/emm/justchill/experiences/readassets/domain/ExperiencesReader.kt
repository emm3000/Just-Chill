package com.emm.justchill.experiences.readassets.domain

import com.emm.justchill.core.FlowResult
import com.emm.justchill.core.asResult

class ExperiencesReader(private val repository: ExperiencesRepository) {

    fun read(): FlowResult<List<Experience>> {
        return repository.readExperiences().asResult()
    }
}