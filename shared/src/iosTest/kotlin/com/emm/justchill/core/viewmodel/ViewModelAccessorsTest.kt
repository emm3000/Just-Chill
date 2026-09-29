package com.emm.justchill.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.KitConfig
import com.emm.justchill.core.initKoin
import com.emm.justchill.core.preferences.appPreferences
import com.emm.justchill.feature.account.AccountsViewModel
import com.emm.justchill.feature.account.AddAccountViewModel
import com.emm.justchill.feature.auth.AuthViewModel
import com.emm.justchill.feature.auth.GoogleSignInLauncher
import com.emm.justchill.feature.auth.GoogleSignInResult
import com.emm.justchill.feature.category.AddCategoryViewModel
import com.emm.justchill.feature.category.CategoriesViewModel
import com.emm.justchill.feature.loan.AddEditLoanViewModel
import com.emm.justchill.feature.loan.LoanDetailViewModel
import com.emm.justchill.feature.loan.LoansViewModel
import com.emm.justchill.feature.loan.PersonLoansViewModel
import com.emm.justchill.feature.profile.ProfileViewModel
import com.emm.justchill.feature.report.ReportViewModel
import com.emm.justchill.feature.transaction.capture.AddTransactionViewModel
import com.emm.justchill.feature.transaction.capture.EditTransactionViewModel
import com.emm.justchill.feature.transaction.list.SeeTransactionsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import org.koin.core.context.stopKoin
import org.koin.mp.KoinPlatform
import kotlin.reflect.KClass
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ViewModelAccessorsTest {

    @BeforeTest
    fun setUp() {
        initKoin(
            KitConfig(
                supabaseUrl = "",
                supabaseAnonKey = "",
                googleServerClientId = "",
                appVersion = "0.0.0-test",
                isSnapshotBackupEnabled = false,
            ),
        )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `every ViewModel resolves through its accessor against the iOS platform module`() {
        val resolved: List<Pair<KClass<*>, MviHandle<*, *, *>>> = listOf(
            AccountsViewModel::class to accountsViewModel(),
            AddAccountViewModel::class to addAccountViewModel(),
            AuthViewModel::class to authViewModel(),
            CategoriesViewModel::class to categoriesViewModel(),
            AddCategoryViewModel::class to addCategoryViewModel(initialType = "Spend", initialName = "Test"),
            LoansViewModel::class to loansViewModel(),
            PersonLoansViewModel::class to personLoansViewModel(personKey = "test-person-key"),
            LoanDetailViewModel::class to loanDetailViewModel(loanId = "test-loan-id"),
            AddEditLoanViewModel::class to addEditLoanViewModel(loanId = null),
            ProfileViewModel::class to profileViewModel(),
            ReportViewModel::class to reportViewModel(),
            AddTransactionViewModel::class to addTransactionViewModel(),
            EditTransactionViewModel::class to editTransactionViewModel(transactionId = "test-transaction-id"),
            SeeTransactionsViewModel::class to seeTransactionsViewModel(),
        )

        resolved.forEach { (type, handle) ->
            assertTrue(type.isInstance(handle.viewModel), "${type.simpleName} resolved to ${handle.viewModel}")
            handle.clear()
        }
        assertEquals(EXPECTED_VIEW_MODEL_COUNT, resolved.map { it.first }.toSet().size)
    }

    @Test
    fun `clear cancels the ViewModel scope`() {
        val handle: MviHandle<*, *, *> = accountsViewModel()
        val scope: CoroutineScope = handle.viewModel.viewModelScope
        assertTrue(scope.isActive)

        handle.clear()

        assertFalse(scope.isActive)
    }

    @Test
    fun `app preferences resolve through their accessor`() {
        appPreferences().firstLaunchSeen
    }

    @Test
    fun `the iOS sign-in launcher answers no credentials`() {
        val launcher: GoogleSignInLauncher = KoinPlatform.getKoin().get()

        val result: GoogleSignInResult = runBlocking { launcher.signIn(serverClientId = "") }

        assertEquals(GoogleSignInResult.NoCredentials, result)
    }

    private companion object {
        const val EXPECTED_VIEW_MODEL_COUNT: Int = 14
    }
}
