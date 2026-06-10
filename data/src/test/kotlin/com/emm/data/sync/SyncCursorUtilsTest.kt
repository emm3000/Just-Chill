package com.emm.data.sync

import org.junit.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pins [parseServerInstant] and [overlapCursor] behaviour.
 *
 * Regression coverage for the production bug where PostgREST returns timestamps with a
 * "+00:00" offset (e.g. "2026-06-10T01:23:45.123456+00:00") that were previously
 * rejected because Instant.parse() only accepts 'Z'-terminated strings — not offset strings.
 * The fix uses OffsetDateTime.parse() which handles both forms.
 */
class SyncCursorUtilsTest {

    // -------------------------------------------------------------------------
    // parseServerInstant — format acceptance
    // -------------------------------------------------------------------------

    @Test
    fun `parseServerInstant accepts PostgREST offset format +00 00`() {
        // This was the production bug: Instant.parse threw on "+00:00" offset form.
        val input = "2026-06-10T01:23:45.123456+00:00"
        val instant = parseServerInstant(input)
        assertNotNull(instant)
        // The resulting instant must represent the same moment as the Z equivalent.
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
        val result = parseServerInstant(input).toString()
        assertTrue(result.endsWith("Z"), "Expected Z-terminated string, got: $result")
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
        assertEquals(null, result)
    }

    @Test
    fun `overlapCursor handles cursor exactly at epoch start`() {
        // 10s before epoch → negative instant, still valid.
        val cursor = "1970-01-01T00:00:10Z"
        val result = overlapCursor(cursor)
        assertEquals("1970-01-01T00:00:00Z", result)
    }
}
