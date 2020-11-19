package com.emm.retrofit.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.emm.retrofit.data.model.Drink
import com.emm.retrofit.domain.Repo
import com.emm.retrofit.vo.Resource

import kotlinx.coroutines.Dispatchers

class MainViewModel(private val repo: Repo) : ViewModel() {

    val fetchTragosList = liveData(Dispatchers.IO) {
        emit(Resource.Loading())
        try {
            emit(repo.getTragoslist("margarita"))
        } catch(ex: Exception) {
            emit(Resource.Failure<List<Drink>>(ex))
        }
    }

}