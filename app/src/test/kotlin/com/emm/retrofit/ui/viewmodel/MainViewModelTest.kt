package com.emm.retrofit.ui.viewmodel

import com.emm.retrofit.MainDispatcherRule
import com.emm.retrofit.core.Result
import com.emm.retrofit.fake.FakeDrinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class MainViewModelTest {

    private lateinit var mainViewModel: MainViewModel
    private val drinkRepository = FakeDrinkRepository()

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mainViewModel = MainViewModel(drinkRepository)
    }

    @Test
    fun `fetchDrinks emits Result Success after repository emits`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            mainViewModel.fetchDrinks.collect()
        }

        drinkRepository.emit()

        assert(mainViewModel.fetchDrinks.value is Result.Success)
    }
}