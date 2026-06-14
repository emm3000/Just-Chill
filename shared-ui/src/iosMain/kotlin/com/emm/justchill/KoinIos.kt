package com.emm.justchill

import com.emm.data.account.AccountLocalDataSource
import com.emm.data.account.DefaultAccountRepository
import com.emm.data.auth.DefaultAuthRepository
import com.emm.data.auth.DefaultClaimLocalDataRepository
import com.emm.data.backup.DefaultBackupRepository
import com.emm.data.category.CategoryLocalDataSource
import com.emm.data.category.DefaultCategoryRepository
import com.emm.data.provideDb
import com.emm.data.provideSqlDriver
import com.emm.data.provideTransactionQueries
import com.emm.data.recurring.DefaultRecurringMovementRepository
import com.emm.data.recurring.RecurringMovementLocalDataSource
import com.emm.data.seedDefaultCategoriesIfEmpty
import com.emm.data.transaction.DefaultTransactionRepository
import com.emm.data.transaction.DefaultTransactionStatsRepository
import com.emm.data.transaction.TransactionLocalDataSource
import com.emm.data.transaction.TransactionStatsLocalDataSource
import com.emm.domain.account.AccountRepository
import com.emm.domain.auth.AuthRepository
import com.emm.domain.auth.ClaimLocalDataOnAuthenticationUseCase
import com.emm.domain.auth.ClaimLocalDataRepository
import com.emm.domain.auth.ClaimLocalDataUseCase
import com.emm.domain.auth.DeleteUserAccountUseCase
import com.emm.domain.auth.ObserveSessionUseCase
import com.emm.domain.auth.ResendConfirmationEmailUseCase
import com.emm.domain.auth.SignInUseCase
import com.emm.domain.auth.SignInWithGoogleUseCase
import com.emm.domain.auth.SignOutUseCase
import com.emm.domain.auth.SignUpUseCase
import com.emm.domain.home.GetHomeDataUseCase
import com.emm.domain.category.CategoryRepository
import com.emm.domain.recurring.RecurringMovementRepository
import com.emm.domain.shared.backup.BackupRepository
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncMutex
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.hh.auth.AuthViewModel
import com.emm.justchill.hh.auth.GoogleSignInLauncher
import com.emm.justchill.hh.di.accountModule
import com.emm.justchill.hh.di.backupModule
import com.emm.justchill.hh.di.categoryModule
import com.emm.justchill.hh.di.homeModule
import com.emm.justchill.hh.di.profileModule
import com.emm.justchill.hh.di.recurringModule
import com.emm.justchill.hh.di.reportModule
import com.emm.justchill.hh.di.seetransactionsModule
import com.emm.justchill.hh.di.sharedModule
import com.emm.justchill.hh.di.transactionModule
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// iOS :data wiring — the platform Koin module. Mirrors :app's dbModule + hhModule (the :data
// repository/datasource binds) but uses the iOS native SQLDelight driver (provideSqlDriver()
// with no Context) instead of AndroidSqliteDriver. Default-category seeding (Android's
// onCreate path) runs here once the DB is built, guarded idempotently.
//
// 6a scope: the local-first closure PLUS real email/password auth + claim-on-sign-in (iosSupabaseModule
// + iosAuthModule). Multi-device SYNC is still stubbed (NoOpSyncCursorStore / NoOpSyncController,
// IosLocalFirstStubs.kt) — that is phase 6b. Google Sign-In + Firebase remain out (deferred).
private val iosDataModule = module {
    single {
        val db = provideDb(provideSqlDriver())
        // iOS equivalent of the Android driver's onCreate seed (no onCreate hook in the native
        // driver). Idempotent: only inserts when no default categories exist yet.
        seedDefaultCategoriesIfEmpty(db)
        db
    }
    single { provideTransactionQueries(get()) }

    // LocalDataSources — most take EmmDatabaseData; the transaction ones take TransactionsQueries.
    factoryOf(::CategoryLocalDataSource)
    factoryOf(::TransactionLocalDataSource)
    factoryOf(::TransactionStatsLocalDataSource)
    factoryOf(::AccountLocalDataSource)
    factoryOf(::RecurringMovementLocalDataSource)

    // Repository binds.
    factoryOf(::DefaultTransactionRepository) { bind<TransactionRepository>() }
    factoryOf(::DefaultTransactionStatsRepository) { bind<TransactionStatsRepository>() }
    factoryOf(::DefaultCategoryRepository) { bind<CategoryRepository>() }
    factoryOf(::DefaultAccountRepository) { bind<AccountRepository>() }
    factoryOf(::DefaultRecurringMovementRepository) { bind<RecurringMovementRepository>() }
    factoryOf(::DefaultBackupRepository) { bind<BackupRepository>() }

    // Home use case — Android binds this in dbModule; the iOS module set mirrors dbModule, so it
    // must bind it too or HomeViewModel (the launch screen) crashes at runtime with NoBeanDefFound.
    factoryOf(::GetHomeDataUseCase)
}

// iOS Supabase client (mirrors :androidApp/hh/di/SupabaseModule.kt). install(Auth) + install(Postgrest)
// + KotlinXSerializer(ignoreUnknownKeys = true). URL/key come from the generated IosSupabaseConfig
// (build/, from root supabase.properties) — the iOS analogue of Android's BuildConfig. When absent
// (CI), URL falls back to the localhost placeholder and network calls surface as
// DomainException.NetworkUnavailable; the app stays fully usable offline/anonymous.
private val iosSupabaseModule = module {
    single { provideSupabaseClient() }
}

private fun provideSupabaseClient(): SupabaseClient {
    // createSupabaseClient requires a non-blank URL; IosSupabaseConfig already applies the
    // empty -> "http://localhost:54321" fallback at generation time (parity with Android's
    // provideSupabaseClient()).
    val url = IosSupabaseConfig.SUPABASE_URL
    val key = IosSupabaseConfig.SUPABASE_ANON_KEY

    return createSupabaseClient(
        supabaseUrl = url,
        supabaseKey = key,
    ) {
        install(Auth)
        install(Postgrest)
        defaultSerializer = KotlinXSerializer(
            json = Json {
                ignoreUnknownKeys = true
            },
        )
    }
}

// iOS auth wiring (mirrors :androidApp/hh/di/AuthModule.kt). DefaultAuthRepository (takes
// SupabaseClient from iosSupabaseModule) + DefaultClaimLocalDataRepository (takes EmmDatabaseData
// from iosDataModule) + all auth use cases + AuthViewModel. Google Sign-In is deferred: the launcher
// is the no-op UnavailableGoogleSignInLauncher and AuthViewModel gets googleServerClientId = "" (the
// Google button is hidden on iOS, so submitWithGoogle short-circuits and never reaches the launcher).
private val iosAuthModule = module {
    factoryOf(::DefaultAuthRepository) { bind<AuthRepository>() }
    factoryOf(::DefaultClaimLocalDataRepository) { bind<ClaimLocalDataRepository>() }

    factoryOf(::UnavailableGoogleSignInLauncher) { bind<GoogleSignInLauncher>() }

    factoryOf(::ClaimLocalDataUseCase)
    factoryOf(::ResendConfirmationEmailUseCase)
    factoryOf(::SignInUseCase)
    factoryOf(::SignInWithGoogleUseCase)
    factoryOf(::SignUpUseCase)
    factoryOf(::SignOutUseCase)
    factoryOf(::ObserveSessionUseCase)
    factoryOf(::ClaimLocalDataOnAuthenticationUseCase)
    factoryOf(::DeleteUserAccountUseCase)

    viewModel {
        AuthViewModel(
            signIn = get(),
            signUp = get(),
            signInWithGoogle = get(),
            resendConfirmationEmail = get(),
            // No web client id on iOS — Google Sign-In is deferred; submitWithGoogle short-circuits
            // on a blank id (and the button is hidden anyway).
            googleServerClientId = "",
            googleSignInLauncher = get(),
        )
    }
}

// iOS profile-support module. Holds what is NOT covered by iosAuthModule: the app-version qualifier,
// the still-stubbed sync ports (NoOpSyncCursorStore / NoOpSyncController — phase 6b), and SyncMutex.
// AuthRepository + ClaimLocalDataRepository are now REAL (iosAuthModule), so ProfileViewModel and
// DeleteUserAccountUseCase resolve against the Supabase-backed impls. See IosLocalFirstStubs.kt.
private val iosProfileSupportModule = module {
    // App version surfaced in the Profile footer.
    single(named("appVersion")) { "1.0.0" }

    // Sync is phase 6b — these stay no-op (replaced with the real Supabase sync engine then).
    single<SyncCursorStore> { NoOpSyncCursorStore() }
    single<SyncController> { NoOpSyncController() }
    single { SyncMutex() }
}

// Application-scoped coroutine scope for the iOS process (analogue of EmmApp.appScope). Drives the
// global session -> claim observer for the lifetime of the app.
private val iosAppScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

// Called once from Swift at app launch (iOSApp.init). Swift sees this top-level fn as
// KoinIosKt.doInitKoin() (the `init` prefix is mangled by the Kotlin/Native Obj-C exporter).
fun initKoin() {
    val koinApp = startKoin {
        modules(
            // Feature modules (commonMain, :domain-only) — full local-first subset.
            transactionModule,
            seetransactionsModule,
            accountModule,
            categoryModule,
            homeModule,
            recurringModule,
            reportModule,
            profileModule,
            backupModule,
            sharedModule,
            // iOS platform wiring.
            iosDataModule,
            iosSupabaseModule,
            iosAuthModule,
            iosProfileSupportModule,
        )
    }

    // Claim-on-sign-in trigger (mirrors EmmApp.onCreate): a long-running observer that stamps
    // anonymous-local rows (userId IS NULL) with the user id whenever the session is Authenticated
    // and unclaimed rows exist. Launched once here because iOS has no Application.onCreate; without
    // it, signing in would authenticate the user but never claim their local data. Resolved from the
    // KoinApplication returned by startKoin (avoids any global-accessor API ambiguity).
    val claimOnAuthentication = koinApp.koin.get<ClaimLocalDataOnAuthenticationUseCase>()
    iosAppScope.launch { claimOnAuthentication() }
}
