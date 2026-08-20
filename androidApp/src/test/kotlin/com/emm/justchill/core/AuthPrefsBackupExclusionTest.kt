package com.emm.justchill.core

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Nothing in the compiler connects the manifest, the extraction rules and the preference file name
 * the session manager writes to. Each one alone silently re-exposes the refresh token.
 */
class AuthPrefsBackupExclusionTest {

    @Test
    fun `cloud backup stays off at the manifest switch`() {
        assertTrue(
            read("src/main/AndroidManifest.xml").contains("""android:allowBackup="false""""),
            "allowBackup is no longer false. Cloud backup would resume, and API 28-30 needs an " +
                "android:fullBackupContent rules file back to keep $AUTH_PREFS_NAME.xml out of it.",
        )
    }

    @Test
    fun `every extraction section excludes the preference file the session manager writes`() {
        val exclusion = """path="$AUTH_PREFS_NAME.xml""""
        val rules = read("src/main/res/xml/data_extraction_rules.xml")

        // cloud-backup and device-transfer are separate sections; neither inherits the other's rules.
        assertEquals(
            2,
            rules.split(exclusion).size - 1,
            "data_extraction_rules.xml no longer excludes $exclusion from both sections. Some " +
                "manufacturers do not let an app opt out of device-to-device migration.",
        )
    }

    private fun read(path: String): String {
        val file = File(path)
        assertTrue(file.isFile, "$path is missing at ${file.absolutePath}.")
        return file.readText()
    }
}
