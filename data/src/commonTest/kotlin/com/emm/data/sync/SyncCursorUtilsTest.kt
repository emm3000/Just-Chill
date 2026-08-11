package com.emm.data.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * Pins [parseServerInstant] and [overlapCursor] behaviour.
 *
 * Regression coverage for the production bug (ADR-002) where PostgREST returns timestamps with a
 * "+00:00" offset (e.g. "2026-06-10T01:23:45.123456+00:00"). kotlinx-datetime's Instant.parse()
 * accepts both 'Z'-terminated and numeric-offset forms and normalises to canonical 'Z' on toString().
 * This suite is the source of truth for cursor format — do not delete or neuter the offset tests.
 */
class SyncCursorUtilsTest {

    // -------------------------------------------------------------------------
    // parseServerInstant — format acceptance
    // -------------------------------------------------------------------------

    @Test
    fun `parseServerInstant accepts PostgREST offset format +00 00`() {
        // This was the production bug: a parser that only accepts 'Z' threw on the "+00:00" form.
        val input = "2026-06-10T01:23:45.123456+00:00"
        val instant = parseServerInstant(input)
        assertNotNull(instant)
        assertEquals(Instant.parse("2026-06-10T01:23:45.123456Z"), instant)
    }

    @Test
    fun `parseServerInstant accepts canonical Z suffix`() {
        val input = "2026-06-10T01:23:45.123456Z"
        val instant = parseServerInstant(input)
        assertEquals(Instant.parse("2026-06-10T01:23:45.123456Z"), instant)
    }

    @Test
    fun `parseServerInstant accepts Z suffix without fractional seconds`() {
        val input = "2026-06-10T01:23:45Z"
        val instant = parseServerInstant(input)
        assertEquals(Instant.parse("2026-06-10T01:23:45Z"), instant)
    }

    @Test
    fun `parseServerInstant converts non-UTC positive offset to correct Instant`() {
        // "+05:00" is UTC+5 — 01:23:45+05:00 == 2026-06-09T20:23:45Z.
        val input = "2026-06-10T01:23:45+05:00"
        val instant = parseServerInstant(input)
        assertEquals(Instant.parse("2026-06-09T20:23:45Z"), instant)
    }

    @Test
    fun `parseServerInstant converts negative offset to correct Instant`() {
        // "-05:00" is UTC-5 — 01:23:45-05:00 == 2026-06-10T06:23:45Z.
        val input = "2026-06-10T01:23:45-05:00"
        val instant = parseServerInstant(input)
        assertEquals(Instant.parse("2026-06-10T06:23:45Z"), instant)
    }

    // -------------------------------------------------------------------------
    // Round-trip canonicalization
    // -------------------------------------------------------------------------

    @Test
    fun `parseServerInstant round-trip offset input produces Z form toString`() {
        // After parse the Instant.toString() must be in canonical Z form, never "+00:00".
        val input = "2026-06-10T01:23:45.123456+00:00"
        val result = assertNotNull(parseServerInstant(input)).toString()
        assertTrue(result.endsWith("Z"), "Expected Z-terminated string, got: $result")
    }

    // -------------------------------------------------------------------------
    // parseServerInstant — input the client cannot read
    // -------------------------------------------------------------------------

    @Test
    fun `parseServerInstant returns null rather than throwing on a malformed value`() {
        // The parse runs inside the per-page database transaction in BaseTableSync. Throwing there
        // takes down the whole pull; returning null lets the row be skipped and the cursor held.
        assertNull(parseServerInstant("not a timestamp"))
        assertNull(parseServerInstant(""))
        assertNull(parseServerInstant("2026-06-10"))
        assertNull(parseServerInstant("2026-13-45T99:99:99Z"))
    }

    @Test
    fun `overlapCursor returns null for a cursor the client cannot parse`() {
        // A stored cursor that cannot be read means "start from the beginning" rather than "fail
        // forever": the full re-pull is idempotent, and LWW makes the re-applied rows a no-op.
        assertNull(overlapCursor("not a timestamp"))
    }

    // -------------------------------------------------------------------------
    // overlapCursor — semantics
    // -------------------------------------------------------------------------

    @Test
    fun `overlapCursor subtracts exactly 10 seconds from the cursor`() {
        val cursor = "2026-06-10T01:00:10Z"
        val result = overlapCursor(cursor)
        // 01:00:10 − 10s = 01:00:00
        assertEquals("2026-06-10T01:00:00Z", result)
    }

    @Test
    fun `overlapCursor with +00 00 offset input subtracts 10 seconds and emits Z form`() {
        val cursor = "2026-06-10T01:00:10.000000+00:00"
        val result = overlapCursor(cursor)
        assertNotNull(result)
        assertEquals("2026-06-10T01:00:00Z", result)
    }

    @Test
    fun `overlapCursor emits canonical Z-terminated ISO string`() {
        val cursor = "2026-06-10T12:30:00Z"
        val result = overlapCursor(cursor)
        assertNotNull(result)
        assertTrue(result.endsWith("Z"), "Expected Z-terminated string, got: $result")
    }

    @Test
    fun `overlapCursor returns null for null cursor`() {
        val result = overlapCursor(null)
        assertNull(result)
    }

    @Test
    fun `overlapCursor handles cursor exactly at epoch start`() {
        // 10s before epoch → negative instant, still valid.
        val cursor = "1970-01-01T00:00:10Z"
        val result = overlapCursor(cursor)
        assertEquals("1970-01-01T00:00:00Z", result)
    }
}
