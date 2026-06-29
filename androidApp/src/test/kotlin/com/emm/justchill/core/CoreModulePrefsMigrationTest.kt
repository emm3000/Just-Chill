package com.emm.justchill.core

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CoreModulePrefsMigrationTest {

    private val editor: SharedPreferences.Editor = mockk(relaxed = true) {
        every { putBoolean(any(), any()) } returns this
        every { putInt(any(), any()) } returns this
        every { putLong(any(), any()) } returns this
        every { putFloat(any(), any()) } returns this
        every { putString(any(), any()) } returns this
        every { putStringSet(any(), any()) } returns this
    }

    private fun legacyWith(entries: Map<String, Any?>): SharedPreferences = mockk {
        every { all } returns entries
    }

    private fun targetPrefs(): SharedPreferences = mockk {
        every { edit() } returns editor
    }

    @Test
    fun `copies Boolean entry with putBoolean`() {
        val legacy = legacyWith(mapOf("first_launch_seen" to true))
        val target = targetPrefs()

        migrateBuildIdPrefs(legacy, target)

        verify { editor.putBoolean("first_launch_seen", true) }
    }

    @Test
    fun `copies String entry with putString`() {
        val legacy = legacyWith(mapOf("last_pulled_at_user123" to "2024-01-01T00:00:00Z"))
        val target = targetPrefs()

        migrateBuildIdPrefs(legacy, target)

        verify { editor.putString("last_pulled_at_user123", "2024-01-01T00:00:00Z") }
    }

    @Test
    fun `copies Long entry with putLong`() {
        val legacy = legacyWith(mapOf("last_synced_at_user123" to 1700000000000L))
        val target = targetPrefs()

        migrateBuildIdPrefs(legacy, target)

        verify { editor.putLong("last_synced_at_user123", 1700000000000L) }
    }

    @Test
    fun `always stamps migration flag and calls apply`() {
        val legacy = legacyWith(mapOf("first_launch_seen" to true))
        val target = targetPrefs()

        migrateBuildIdPrefs(legacy, target)

        verify { editor.putBoolean("_migrated_from_build_id", true) }
        verify { editor.apply() }
    }

    @Test
    fun `empty legacy still stamps flag and applies`() {
        val legacy = legacyWith(emptyMap())
        val target = targetPrefs()

        migrateBuildIdPrefs(legacy, target)

        verify { editor.putBoolean("_migrated_from_build_id", true) }
        verify { editor.apply() }
    }
}
