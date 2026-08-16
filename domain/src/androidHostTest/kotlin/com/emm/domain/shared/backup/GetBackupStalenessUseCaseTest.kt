package com.emm.domain.shared.backup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class GetBackupStalenessUseCaseTest {

    private val lima = TimeZone.of("America/Lima")

    private val madrid = TimeZone.of("Europe/Madrid")

    private val backupRepository = mockk<BackupRepository>()

    private fun clockAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Clock = object : Clock {
        override fun now(): Instant = LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone)
    }

    private fun millisAt(date: LocalDate, hour: Int, minute: Int, zone: TimeZone): Long =
        LocalDateTime(date, LocalTime(hour, minute)).toInstant(zone).toEpochMilliseconds()

    private fun useCase(clock: Clock, zone: TimeZone = lima) = GetBackupStalenessUseCase(
        backupRepository = backupRepository,
        clock = clock,
        timeZone = zone,
    )

    @Test
    fun `exactly three days old is NOT stale, even with local changes pending`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 8), 9, 0, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup + ONE_HOUR_MILLIS

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 23, 59, lima)).invoke(lastBackup)

        assertEquals(BACKUP_STALE_AFTER_DAYS, result.daysSinceLastBackup)
        assertFalse(result.isStale, "The plan says staleness must EXCEED three days, not reach it.")
    }

    @Test
    fun `four days old with local changes IS stale`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 7), 9, 0, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup + ONE_HOUR_MILLIS

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 0, 1, lima)).invoke(lastBackup)

        assertEquals(4, result.daysSinceLastBackup)
        assertTrue(result.isStale)
    }

    @Test
    fun `the threshold is counted in calendar days, not in elapsed hours`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 8), 23, 59, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup + ONE_HOUR_MILLIS

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 12), 0, 1, lima)).invoke(lastBackup)

        assertEquals(4, result.daysSinceLastBackup)
        assertTrue(result.isStale)
    }

    @Test
    fun `an old backup with nothing changed since is NOT stale`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.JULY, 1), 9, 0, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup - ONE_HOUR_MILLIS

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 12, 0, lima)).invoke(lastBackup)

        assertEquals(41, result.daysSinceLastBackup)
        assertFalse(result.isStale, "A ledger nobody has touched is completely backed up, however old.")
    }

    @Test
    fun `a local change at the exact instant of the backup is NOT a change since it`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.JULY, 1), 9, 0, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 12, 0, lima)).invoke(lastBackup)

        assertFalse(result.isStale)
    }

    @Test
    fun `an empty database is never stale, however old the last backup is`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.JANUARY, 1), 9, 0, lima)
        coEvery { backupRepository.latestLocalChangeAt() } returns null

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 12, 0, lima)).invoke(lastBackup)

        assertEquals(222, result.daysSinceLastBackup)
        assertFalse(result.isStale, "An empty ledger has nothing a snapshot could be missing.")
    }

    @Test
    fun `the database is not read at all while the age half already says no`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 10), 9, 0, lima)

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 12, 0, lima)).invoke(lastBackup)

        assertFalse(result.isStale)
        coVerify(exactly = 0) { backupRepository.latestLocalChangeAt() }
    }

    @Test
    fun `a backup earlier today is zero days old`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 11), 0, 5, lima)

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 23, 55, lima)).invoke(lastBackup)

        assertEquals(0, result.daysSinceLastBackup)
        assertFalse(result.isStale)
    }

    @Test
    fun `a backup last night is one day old, however few hours ago it was`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 10), 23, 55, lima)

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 0, 5, lima)).invoke(lastBackup)

        assertEquals(1, result.daysSinceLastBackup)
    }

    @Test
    fun `a watermark in the future reads as today rather than as a negative age`() = runTest {
        val lastBackup = millisAt(LocalDate(2026, Month.AUGUST, 20), 9, 0, lima)

        val result = useCase(clockAt(LocalDate(2026, Month.AUGUST, 11), 12, 0, lima)).invoke(lastBackup)

        assertEquals(0, result.daysSinceLastBackup, "\"hace -9 días\" is not a sentence.")
        assertFalse(result.isStale)
    }

    @Test
    fun `one instant, two zones, two different verdicts`() = runTest {
        val now = object : Clock {
            override fun now(): Instant = Instant.parse("2026-08-12T02:00:00Z")
        }
        val lastBackup = Instant.parse("2026-08-08T12:00:00Z").toEpochMilliseconds()
        coEvery { backupRepository.latestLocalChangeAt() } returns lastBackup + ONE_HOUR_MILLIS

        val inLima = useCase(now, lima).invoke(lastBackup)
        val inMadrid = useCase(now, madrid).invoke(lastBackup)

        assertEquals(3, inLima.daysSinceLastBackup)
        assertFalse(inLima.isStale, "Three days in Lima is the threshold, not past it.")

        assertEquals(4, inMadrid.daysSinceLastBackup)
        assertTrue(inMadrid.isStale, "The same instant is a day further along in Madrid.")
    }

    private companion object {
        const val ONE_HOUR_MILLIS: Long = 60L * 60L * 1000L
    }
}
