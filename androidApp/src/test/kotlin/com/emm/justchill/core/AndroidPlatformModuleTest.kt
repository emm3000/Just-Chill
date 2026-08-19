package com.emm.justchill.core

import com.emm.justchill.BuildInfo
import org.junit.Test
import org.koin.dsl.koinApplication
import kotlin.test.assertEquals

class AndroidPlatformModuleTest {

    @Test
    fun `androidPlatformModule binds the commit hash the nav host resolves at launch`() {
        // Koin's `single { }` bindings resolve lazily, so asking only for CommitHash never touches
        // the Context-dependent ones (SQLDelight driver, SharedPreferencesSettings, sign-in launcher).
        val koin = koinApplication { modules(androidPlatformModule) }.koin

        try {
            assertEquals(
                CommitHash(BuildInfo.commitHash),
                koin.get<CommitHash>(),
                "androidPlatformModule no longer produces the generated commit hash as a CommitHash.",
            )
        } finally {
            koin.close()
        }
    }
}
