package com.emm.justchill.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.emm.justchill.core.domain.shared.Money
import com.emm.justchill.core.ui.atoms.BackBtn
import com.emm.justchill.core.ui.atoms.JcTopBar
import com.emm.justchill.core.ui.theme.EmmColors
import com.emm.justchill.core.ui.theme.EmmSpacing
import com.emm.justchill.core.ui.theme.EmmTheme
import com.emm.justchill.core.ui.theme.LocalEmmColors
import com.emm.justchill.core.ui.theme.LocalEmmSpacing

@Composable
fun ProfileScreen(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    appVersion: String = "",
    commitHash: String = "",
    onBack: () -> Unit,
    onTransactionsClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
    onAccountsClick: () -> Unit = {},
    onCategoriesClick: () -> Unit = {},
    onRecurringClick: () -> Unit = {},
    onLoansClick: () -> Unit = {},
    onAboutClick: () -> Unit = {},
    onExportClick: () -> Unit = {},
    onImportClick: () -> Unit = {},
    onImportConfirm: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onDeleteAccountConfirm: () -> Unit = {},
    onDialogDismiss: () -> Unit = {},
    onCopyCommitHashClick: () -> Unit = {},
    onBackUpNowClick: () -> Unit = {},
    onVerifyBackupClick: () -> Unit = {},
    onAcknowledgeBackupDestinationClick: () -> Unit = {},
) {
    val colors: EmmColors = LocalEmmColors.current
    val spacing: EmmSpacing = LocalEmmSpacing.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.bg)
            .verticalScroll(rememberScrollState()),
    ) {
        JcTopBar(
            title = "Más",
            left = { BackBtn(onClick = onBack) },
        )

        (state.session as? SessionUiState.SignedIn)?.let { signedIn ->
            AccountSection(
                session = signedIn,
                op = state.op,
                dialog = state.dialog,
                onSignOutClick = onSignOutClick,
                onDeleteAccountClick = onDeleteAccountClick,
                onDeleteAccountConfirm = onDeleteAccountConfirm,
                onDialogDismiss = onDialogDismiss,
            )
        }

        DestinationsSection(
            categoryCount = state.categoryCount,
            incomeCategoryCount = state.incomeCategoryCount,
            recurringCount = state.recurringCount,
            recurringMonthlyOutflow = state.recurringMonthlyOutflow,
            destinations = ProfileDestinationActions(
                onTransactionsClick = onTransactionsClick,
                onReportClick = onReportClick,
                onAccountsClick = onAccountsClick,
                onCategoriesClick = onCategoriesClick,
                onRecurringClick = onRecurringClick,
                onLoansClick = onLoansClick,
            ),
        )

        BackupSection(
            state = state,
            onExportClick = onExportClick,
            onImportClick = onImportClick,
            onImportConfirm = onImportConfirm,
            onDialogDismiss = onDialogDismiss,
            onSignInClick = onSignInClick,
            snapshotActions = SnapshotBackupActions(
                onBackUpNow = onBackUpNowClick,
                onVerify = onVerifyBackupClick,
                onAcknowledgeDestination = onAcknowledgeBackupDestinationClick,
            ),
        )

        AppSection(
            appVersion = appVersion,
            onAboutClick = onAboutClick,
            onPrivacyClick = onPrivacyClick,
        )

        Spacer(Modifier.height(spacing.s6))
        CommitFooter(commitHash = commitHash, onCopyClick = onCopyCommitHashClick)
        Spacer(Modifier.height(spacing.s4))
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    EmmTheme {
        ProfileScreen(
            state = ProfileUiState(
                categoryCount = 24,
                incomeCategoryCount = 7,
                recurringCount = 3,
                recurringMonthlyOutflow = Money(9_000L),
                lastExport = LastExportUi.DaysAgo(3),
                session = SessionUiState.SignedOut,
            ),
            onBack = {},
            appVersion = "1.0.0",
            commitHash = "4e47828d1f2a3b4c5d6e7f8091a2b3c4d5e6f708",
        )
    }
}
