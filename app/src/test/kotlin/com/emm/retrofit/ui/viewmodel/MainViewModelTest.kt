package com.emm.retrofit.ui.viewmodel

import com.emm.retrofit.MainDispatcherRule
import com.emm.retrofit.core.Result
import com.emm.retrofit.experiences.drinks.domain.DrinkFetcher
import com.emm.retrofit.experiences.drinks.ui.MainViewModel
import com.emm.retrofit.fake.FakeDrinkRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
        mainViewModel = MainViewModel(DrinkFetcher(drinkRepository))
    }

    @Test
    fun `fetchDrinks emits Result Success after repository emits`() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            mainViewModel.fetchDrinks.collect()
        }

        delay(400L) // delay for debounce operator
        drinkRepository.emit()

        assert(mainViewModel.fetchDrinks.value is Result.Success)
    }
}