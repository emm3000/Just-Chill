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
import com.emm.data.sync.AccountTableSync
import com.emm.data.sync.CategoryTableSync
import com.emm.data.sync.DefaultSyncRepository
import com.emm.data.sync.RecurringMovementTableSync
import com.emm.data.sync.TableSync
import com.emm.data.sync.TransactionTableSync
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
import com.emm.domain.sync.ConflictResolver
import com.emm.domain.sync.ObservePendingSyncCountUseCase
import com.emm.domain.sync.SyncCursorStore
import com.emm.domain.sync.SyncDataUseCase
import com.emm.domain.sync.SyncMutex
import com.emm.domain.sync.SyncRepository
import com.emm.domain.transaction.TransactionRepository
import com.emm.domain.transaction.TransactionStatsRepository
import com.emm.justchill.core.sync.IosSyncCursorStore
import com.emm.justchill.core.sync.IosSyncOrchestrator
import com.emm.justchill.core.sync.SyncController
import com.emm.justchill.core.sync.iosForegroundEvents
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
import io.github.jan.supabase.annotations.SupabaseExperimental
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
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

// iOS :data wiring — the platform Koin module. Mirrors :app's dbModule + hhModule (the :data
// repository/datasource binds) but uses the iOS native SQLDelight driver (provideSqlDriver()
// with no Context) instead of AndroidSqliteDriver. Default-category seeding (Android's
// onCreate path) runs here once the DB is built, guarded idempotently.
//
// 6a scope: the local-first closure PLUS real email/password auth + claim-on-sign-in (iosSupabaseModule
// + iosAuthModule). 6b adds MANUAL multi-device sync (iosSyncModule: IosSyncCursorStore over
// NSUserDefaults + IosSyncOrchestrator, manual + sign-in triggers). 6c completes the automatic-sync
// lifecycle (on-resume via UIApplicationDidBecomeActive + debounced writes), reaching Android parity.
// Google Sign-In + Firebase remain out (deferred).
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
        install(Postgrest) {
            // Require an authenticated session for every Postgrest request: never fall back to the
            // anon supabaseKey. Postgrest is used ONLY by the sync engine and the delete_account RPC,
            // both strictly authenticated paths. Without this, an unresolved JWT is silently
            // downgraded to an anonymous request that RLS rejects with a confusing HTTP 403; with it,
            // the call throws SessionRequiredException, which maps to a retryable sync error instead.
            @OptIn(SupabaseExperimental::class)
            requireValidSession = true
        }
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

// iOS profile-support module. Holds what is NOT covered by iosAuthModule or iosSyncModule: the
// app-version qualifier and the single SyncMutex (one lock per process — serializes sync cycles
// against each other AND against account deletion; reused by SyncDataUseCase and DeleteUserAccountUseCase).
// AuthRepository + ClaimLocalDataRepository are REAL (iosAuthModule), so ProfileViewModel and
// DeleteUserAccountUseCase resolve against the Supabase-backed impls.
private val iosProfileSupportModule = module {
    // App version surfaced in the Profile footer.
    single(named("appVersion")) { "1.0.0" }

    // Single: ONE lock per process. Serializes sync cycles against each other AND against account
    // deletion (in-flight push must not resurrect rows after delete_account). Reused by SyncDataUseCase.
    single { SyncMutex() }
}

// Qualifier for the single application-lifetime coroutine scope (mirrors Android's appScopeQualifier).
// One scope drives BOTH the claim-on-sign-in observer (initKoin) and the sync orchestrator loops.
private val appScopeQualifier = named("appScope")

// iOS sync wiring — mirrors :androidApp/hh/di/SyncModule.kt. The commonMain sync engine
// (DefaultSyncRepository, BaseTableSync, SyncDataUseCase) is already shared via :data; this module only
// supplies the four qualified TableSync units, the cursor store + conflict resolver, the use cases, the
// app scope, and the iOS orchestrator. As of slice 6c IosSyncOrchestrator runs the full automatic-sync
// trigger set (manual + sign-in + on-resume + debounced writes), at parity with Android. Only the
// connectivity-regained trigger is absent on both platforms (shared cross-platform debt).
private val iosSyncModule = module {
    // Per-table sync units — qualified so DefaultSyncRepository can distinguish the four TableSync
    // slots even though they share the interface type. SAME qualifier strings as Android's SyncModule.
    // Each takes (EmmDatabaseData, SupabaseClient) — bound by iosDataModule + iosSupabaseModule.
    factory<TableSync>(named("accountSync")) { AccountTableSync(get(), get()) }
    factory<TableSync>(named("categorySync")) { CategoryTableSync(get(), get()) }
    factory<TableSync>(named("transactionSync")) { TransactionTableSync(get(), get()) }
    factory<TableSync>(named("recurringSync")) { RecurringMovementTableSync(get(), get()) }

    // Domain port: cursor store over NSUserDefaults (replaces the 6a NoOpSyncCursorStore).
    single<SyncCursorStore> { IosSyncCursorStore() }

    // DefaultSyncRepository dependency — easy to miss; omitting it crashes at first sync resolution.
    factory { ConflictResolver() }

    // FK-safe ordering preserved: accounts → categories → transactions → recurring_movements.
    factory<SyncRepository> {
        DefaultSyncRepository(
            observeSession = get(),
            cursorStore = get(),
            conflictResolver = get(),
            accountSync = get(named("accountSync")),
            categorySync = get(named("categorySync")),
            transactionSync = get(named("transactionSync")),
            recurringSync = get(named("recurringSync")),
        )
    }

    singleOf(::SyncDataUseCase)

    // Consumed by IosSyncOrchestrator's debounced-writes trigger (slice 6c) — the same use case the
    // Android SyncOrchestrator uses for its trigger (c).
    factoryOf(::ObservePendingSyncCountUseCase)

    // Single application-lifetime scope (analogue of EmmApp.appScope). Owns the orchestrator loops
    // AND the claim observer (see initKoin) — ONE scope, not two.
    single<CoroutineScope>(appScopeQualifier) {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    // Single: owns the long-lived consumer + the four trigger loops (manual/sign-in/on-resume/debounced
    // writes), all launched in its init{} on appScope. Bound to SyncController so commonMain consumers
    // (ProfileViewModel) resolve the same instance (replaces the 6a NoOpSyncController). Binding it here
    // starts the loops. resumeEvents = iosForegroundEvents() supplies the on-resume signal
    // (UIApplicationDidBecomeActive) — the iOS analogue of Android's ProcessLifecycleOwner ON_RESUME flow.
    single<SyncController> {
        IosSyncOrchestrator(
            syncData = get(),
            observeSession = get(),
            observePendingCount = get(),
            signOut = get(),
            appScope = get(appScopeQualifier),
            resumeEvents = iosForegroundEvents(),
        )
    }
}

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
            iosSyncModule,
        )
    }

    val koin = koinApp.koin

    // The single application-lifetime scope (analogue of EmmApp.appScope). ONE scope drives both the
    // claim-on-sign-in observer below AND the sync orchestrator's internal loops.
    val appScope = koin.get<CoroutineScope>(appScopeQualifier)

    // Claim-on-sign-in trigger (mirrors EmmApp.onCreate): a long-running observer that stamps
    // anonymous-local rows (userId IS NULL) with the user id whenever the session is Authenticated
    // and unclaimed rows exist. Launched once here because iOS has no Application.onCreate; without
    // it, signing in would authenticate the user but never claim their local data.
    val claimOnAuthentication = koin.get<ClaimLocalDataOnAuthenticationUseCase>()
    appScope.launch { claimOnAuthentication() }

    // Eagerly resolve the orchestrator so its init{} starts the consumer + sign-in observer NOW.
    // A Koin `single` is lazy: without this, the loops would not start until the Profile screen first
    // resolves SyncController — so a sign-in from the Auth screen (before visiting Perfil) would not
    // trigger sync. Mirrors EmmApp calling SyncOrchestrator.start() at launch. The instance is shared,
    // so ProfileViewModel later resolves this very same started orchestrator.
    koin.get<SyncController>()
}
