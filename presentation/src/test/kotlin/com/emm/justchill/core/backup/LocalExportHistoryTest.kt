package com.emm.justchill.core.backup

import com.russhwolf.settings.MapSettings
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class LocalExportHistoryTest {

    private val lima = TimeZone.of("America/Lima")

    private fun historyAt(now: String, settings: MapSettings = MapSettings()) = LocalExportHistory(
        settings = settings,
        clock = object : Clock {
            override fun now(): Instant = Instant.parse(now)
        },
        timeZone = lima,
    )

    @Test
    fun `never exported reads as null, not as zero days`() {
        assertNull(historyAt("2026-08-28T15:00:00Z").daysSinceLastExport())
    }

    @Test
    fun `an export recorded today is zero days old`() {
        val settings = MapSettings()
        historyAt("2026-08-28T15:00:00Z", settings).recordExport()

        assertEquals(0, historyAt("2026-08-28T23:00:00Z", settings).daysSinceLastExport())
    }

    @Test
    fun `the age counts calendar days in the injected zone, not elapsed hours`() {
        val settings = MapSettings()
        // 20:00 in Lima on the 27th; 03:00 UTC on the 28th. A UTC reading would call this same-day.
        historyAt("2026-08-28T01:00:00Z", settings).recordExport()

        assertEquals(1, historyAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport())
    }

    @Test
    fun `an export three days back reads as three`() {
        val settings = MapSettings()
        historyAt("2026-08-25T15:00:00Z", settings).recordExport()

        assertEquals(3, historyAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport())
    }

    @Test
    fun `a clock that moved backwards never reports a negative age`() {
        val settings = MapSettings()
        historyAt("2026-08-28T15:00:00Z", settings).recordExport()

        assertEquals(0, historyAt("2026-08-20T15:00:00Z", settings).daysSinceLastExport())
    }

    @Test
    fun `recording again replaces the previous export`() {
        val settings = MapSettings()
        historyAt("2026-08-20T15:00:00Z", settings).recordExport()
        historyAt("2026-08-27T15:00:00Z", settings).recordExport()

        assertEquals(1, historyAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport())
    }
}
