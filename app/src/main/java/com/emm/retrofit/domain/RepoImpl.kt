package com.emm.retrofit.domain

import com.emm.retrofit.data.DataSource
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.vo.Resource

class RepoImpl(private val dataSource: DataSource): Repo {

    override suspend fun getTragoslist(tragoName: String): Resource<List<Drink>> {
        return dataSource.getTragoByName(tragoName)
    }

}