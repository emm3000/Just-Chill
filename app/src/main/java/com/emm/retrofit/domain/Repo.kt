package com.emm.retrofit.domain

import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.vo.Resource

interface Repo {
    suspend fun getTragoslist(trangoName: String) : Resource<List<Drink>>
}