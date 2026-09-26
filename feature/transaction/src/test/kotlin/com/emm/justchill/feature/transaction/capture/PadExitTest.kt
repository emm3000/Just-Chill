package com.emm.justchill.feature.transaction.capture

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import com.emm.justchill.core.domain.account.Account
import com.emm.justchill.core.domain.shared.AccountId
import com.emm.justchill.core.ui.theme.EmmTheme
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@RunWith(RobolectricTestRunner::class)
class PadExitTest {

    @get:Rule
    val composeRule: ComposeContentTestRule = createComposeRule()

    private val lima: TimeZone = TimeZone.of("America/Lima")
    private val today: LocalDate = LocalDate(2026, Month.AUGUST, 10)
    private val onlyAccount: Account = Account(AccountId("yape"), "Yape")

    private val fixedClock: Clock = object : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(
            LocalDateTime(today, LocalTime(14, 30)).toInstant(lima).toEpochMilliseconds(),
        )
    }

    private fun padViewModel(): AddTransactionViewModel = addTransactionViewModel(
        todayDates = MutableStateFlow(today),
        clock = fixedClock,
        zone = lima,
        accountRepository = mockk { every { all() } returns flowOf(listOf(onlyAccount)) },
        categoryRepository = mockk { every { all() } returns flowOf(emptyList()) },
    )

    @Test
    fun `the close key leaves the pad once`() {
        var closed: Int = 0

        composeRule.setContent {
            EmmTheme {
                AddTransactionScreenContent(
                    state = AddTransactionUiState(today = today),
                    onIntent = {},
                    onClose = { closed += 1 },
                    onOpenTransactions = {},
                    onSave = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Cerrar").performClick()

        assertEquals(1, closed)
    }

    @Test
    fun `a saved movement leaves through the save exit, never through the close key`() {
        val vm: AddTransactionViewModel = padViewModel()
        val exits: MutableList<String> = mutableListOf()

        composeRule.setContent {
            EmmTheme {
                AddTransactionScreen(
                    vm = vm,
                    onClose = { exits += "close" },
                    onSaveSuccess = { exits += "saved" },
                    snackbarHostState = SnackbarHostState(),
                    onOpenTransactions = {},
                )
            }
        }
        composeRule.waitUntil { vm.state.value.accountSelected == onlyAccount }

        vm.onIntent(AddTransactionIntent.OnAmountChange("8540"))
        vm.onIntent(AddTransactionIntent.OnSave)
        composeRule.waitUntil { exits.isNotEmpty() }
        composeRule.waitForIdle()

        assertEquals(listOf("saved"), exits)
    }
}
