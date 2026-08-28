package com.emm.justchill.core.backup

import com.russhwolf.settings.MapSettings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class LocalExportHistoryTest {

    private val lima = TimeZone.of("America/Lima")

    private val august28 = LocalDate(2026, 8, 28)

    // The clock stamps writes only; the day a read is measured against arrives as an argument.
    private fun writingAt(now: String, settings: MapSettings = MapSettings()) = LocalExportHistory(
        settings = settings,
        clock = object : Clock {
            override fun now(): Instant = Instant.parse(now)
        },
        timeZone = lima,
    )

    @Test
    fun `never exported reads as null, not as zero days`() {
        assertNull(writingAt("2026-08-28T15:00:00Z").daysSinceLastExport(august28))
    }

    @Test
    fun `an export recorded today is zero days old`() {
        val settings = MapSettings()
        writingAt("2026-08-28T15:00:00Z", settings).recordExport()

        assertEquals(0, writingAt("2026-08-28T23:00:00Z", settings).daysSinceLastExport(august28))
    }

    @Test
    fun `one stored export keeps ageing as the day it is measured against moves`() {
        val settings = MapSettings()
        writingAt("2026-08-28T15:00:00Z", settings).recordExport()
        val history = writingAt("2026-08-28T15:00:00Z", settings)

        assertEquals(0, history.daysSinceLastExport(august28))
        assertEquals(1, history.daysSinceLastExport(LocalDate(2026, 8, 29)))
        assertEquals(2, history.daysSinceLastExport(LocalDate(2026, 8, 30)))
    }

    @Test
    fun `the stored instant lands on its own day in the injected zone, not in UTC`() {
        val settings = MapSettings()
        // 20:00 in Lima on the 27th, 01:00 UTC on the 28th: a UTC reading would call this same-day.
        writingAt("2026-08-28T01:00:00Z", settings).recordExport()

        assertEquals(1, writingAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport(august28))
    }

    @Test
    fun `an export three days back reads as three`() {
        val settings = MapSettings()
        writingAt("2026-08-25T15:00:00Z", settings).recordExport()

        assertEquals(3, writingAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport(august28))
    }

    @Test
    fun `a day earlier than the export never reports a negative age`() {
        val settings = MapSettings()
        writingAt("2026-08-28T15:00:00Z", settings).recordExport()

        assertEquals(0, writingAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport(LocalDate(2026, 8, 20)))
    }

    @Test
    fun `recording again replaces the previous export`() {
        val settings = MapSettings()
        writingAt("2026-08-20T15:00:00Z", settings).recordExport()
        writingAt("2026-08-27T15:00:00Z", settings).recordExport()

        assertEquals(1, writingAt("2026-08-28T15:00:00Z", settings).daysSinceLastExport(august28))
    }
}
