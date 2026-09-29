package com.emm.justchill.core.viewmodel

import androidx.lifecycle.viewModelScope
import com.emm.justchill.core.KitConfig
import com.emm.justchill.core.initKoin
import com.emm.justchill.core.preferences.resolveAppPreferences
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
            AccountsViewModel::class to resolveAccountsHandle(),
            AddAccountViewModel::class to resolveAddAccountHandle(),
            AuthViewModel::class to resolveAuthHandle(),
            CategoriesViewModel::class to resolveCategoriesHandle(),
            AddCategoryViewModel::class to resolveAddCategoryHandle(initialType = "Spend", initialName = "Test"),
            LoansViewModel::class to resolveLoansHandle(),
            PersonLoansViewModel::class to resolvePersonLoansHandle(personKey = "test-person-key"),
            LoanDetailViewModel::class to resolveLoanDetailHandle(loanId = "test-loan-id"),
            AddEditLoanViewModel::class to resolveAddEditLoanHandle(loanId = null),
            ProfileViewModel::class to resolveProfileHandle(),
            ReportViewModel::class to resolveReportHandle(),
            AddTransactionViewModel::class to resolveAddTransactionHandle(),
            EditTransactionViewModel::class to resolveEditTransactionHandle(transactionId = "test-transaction-id"),
            SeeTransactionsViewModel::class to resolveSeeTransactionsHandle(),
        )

        resolved.forEach { (type, handle) ->
            assertTrue(type.isInstance(handle.viewModel), "${type.simpleName} resolved to ${handle.viewModel}")
            handle.clear()
        }
    }

    @Test
    fun `clear cancels the ViewModel scope`() {
        val handle: MviHandle<*, *, *> = resolveAccountsHandle()
        val scope: CoroutineScope = handle.viewModel.viewModelScope
        assertTrue(scope.isActive)

        handle.clear()

        assertFalse(scope.isActive)
    }

    @Test
    fun `app preferences resolve through their accessor onto the platform settings`() {
        val original: Boolean = resolveAppPreferences().firstLaunchSeen
        resolveAppPreferences().firstLaunchSeen = !original

        val reread: Boolean = resolveAppPreferences().firstLaunchSeen
        resolveAppPreferences().firstLaunchSeen = original

        assertEquals(!original, reread)
    }

    @Test
    fun `the iOS sign-in launcher answers no credentials`() {
        val launcher: GoogleSignInLauncher = KoinPlatform.getKoin().get()

        val result: GoogleSignInResult = runBlocking { launcher.signIn(serverClientId = "") }

        assertEquals(GoogleSignInResult.NoCredentials, result)
    }
}
