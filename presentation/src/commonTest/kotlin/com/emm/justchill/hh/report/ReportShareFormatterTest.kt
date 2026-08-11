package com.emm.justchill.hh.report

import com.emm.domain.shared.YearMonth
import kotlinx.datetime.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReportShareFormatterTest {

    // Which month it is does not matter to a formatter — that it is STATED does. ReportUiState used
    // to default it to YearMonth.current(), so these tests silently ran against the wall clock.
    private val may2026 = YearMonth(2026, Month.MAY)

    // ── buildContextSentence ──────────────────────────────────────────────

    @Test
    fun `buildContextSentence with null delta returns base sentence only`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = 30, deltaPoints = null)
        assertEquals("De cada S/ 100 que entró, ahorraste S/ 30.", result)
    }

    @Test
    fun `buildContextSentence with positive delta appends improvement text`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = 40, deltaPoints = 5)
        assertEquals(
            "De cada S/ 100 que entró, ahorraste S/ 40. Mejoraste vs. los 6 meses previos.",
            result,
        )
    }

    @Test
    fun `buildContextSentence with negative delta appends worsening text`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = 20, deltaPoints = -3)
        assertEquals(
            "De cada S/ 100 que entró, ahorraste S/ 20. Empeoraste vs. los 6 meses previos.",
            result,
        )
    }

    @Test
    fun `buildContextSentence with zero delta appends same-pace text`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = 25, deltaPoints = 0)
        assertEquals(
            "De cada S/ 100 que entró, ahorraste S/ 25. Mantuviste el mismo ritmo que los 6 meses previos.",
            result,
        )
    }

    @Test
    fun `buildContextSentence with a negative rate says what was overspent`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = -50, deltaPoints = null)
        // "ahorraste S/ -50" is not a sentence. At -50% the user spent 150 for every 100 earned.
        assertEquals("De cada S/ 100 que entró, gastaste S/ 150.", result)
    }

    @Test
    fun `buildContextSentence with a negative rate still appends the comparison`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = -20, deltaPoints = 8)
        assertEquals(
            "De cada S/ 100 que entró, gastaste S/ 120. Mejoraste vs. los 6 meses previos.",
            result,
        )
    }

    @Test
    fun `buildContextSentence at exactly zero still reads as savings`() {
        val result = ReportShareFormatter.buildContextSentence(ratePercent = 0, deltaPoints = null)
        assertEquals("De cada S/ 100 que entró, ahorraste S/ 0.", result)
    }

    // ── buildTopMetaText ──────────────────────────────────────────────────

    @Test
    fun `buildTopMetaText formats months-in-top label correctly`() {
        assertEquals("Top en 4 de 6 meses", ReportShareFormatter.buildTopMetaText(4, 6))
    }

    @Test
    fun `buildTopMetaText with monthsInTop equal to totalMonths`() {
        assertEquals("Top en 6 de 6 meses", ReportShareFormatter.buildTopMetaText(6, 6))
    }

    @Test
    fun `buildTopMetaText with monthsInTop zero`() {
        assertEquals("Top en 0 de 6 meses", ReportShareFormatter.buildTopMetaText(0, 6))
    }

    // ── buildMesShareText ─────────────────────────────────────────────────

    @Test
    fun `buildMesShareText contains JustChill footer`() {
        val state = ReportUiState(month = may2026)
        val result = ReportShareFormatter.buildMesShareText(state)
        assertTrue(result.contains("— JustChill"), "Footer missing from: $result")
    }

    @Test
    fun `buildMesShareText includes singular movimiento when count is 1`() {
        val state = ReportUiState(month = may2026, movementCount = 1, averageFormatted = "S/ 500")
        val result = ReportShareFormatter.buildMesShareText(state)
        assertTrue(result.contains("1 movimiento"), "Expected singular 'movimiento' in: $result")
    }

    @Test
    fun `buildMesShareText includes plural movimientos when count is not 1`() {
        val state = ReportUiState(month = may2026, movementCount = 5, averageFormatted = "S/ 200")
        val result = ReportShareFormatter.buildMesShareText(state)
        assertTrue(result.contains("5 movimientos"), "Expected plural 'movimientos' in: $result")
    }

    // ── buildTrendsShareText ──────────────────────────────────────────────

    @Test
    fun `buildTrendsShareText contains JustChill footer`() {
        val state = ReportUiState(month = may2026)
        val result = ReportShareFormatter.buildTrendsShareText(state)
        assertTrue(result.contains("— JustChill"), "Footer missing from: $result")
    }

    @Test
    fun `buildTrendsShareText includes savings rate percent`() {
        val state = ReportUiState(
            month = may2026,
            trends = TrendsUiData(savingsRatePercent = 35),
        )
        val result = ReportShareFormatter.buildTrendsShareText(state)
        assertTrue(result.contains("35%"), "Expected rate '35%' in: $result")
    }

    @Test
    fun `buildTrendsShareText signs the delta itself, since plain text has no arrow icon`() {
        // The pill on screen gets its direction from a leading icon, so deltaText carries no
        // glyph. Shared text has no icon to lean on: "(10 pts vs. 6 meses previos)" would not
        // say whether the user improved or slipped.
        val worse = ReportUiState(
            month = may2026,
            trends = TrendsUiData(savingsRatePercent = 20, deltaText = "10 pts", deltaIsPositive = false),
        )
        val better = ReportUiState(
            month = may2026,
            trends = TrendsUiData(savingsRatePercent = 40, deltaText = "10 pts", deltaIsPositive = true),
        )

        assertTrue(
            ReportShareFormatter.buildTrendsShareText(worse).contains("(↓ 10 pts vs. 6 meses previos)"),
            "Expected a down arrow in: ${ReportShareFormatter.buildTrendsShareText(worse)}",
        )
        assertTrue(
            ReportShareFormatter.buildTrendsShareText(better).contains("(↑ 10 pts vs. 6 meses previos)"),
            "Expected an up arrow in: ${ReportShareFormatter.buildTrendsShareText(better)}",
        )
    }

    @Test
    fun `buildTrendsShareText omits the delta entirely when there is no baseline`() {
        val state = ReportUiState(month = may2026, trends = TrendsUiData(savingsRatePercent = 20, deltaText = null))
        val result = ReportShareFormatter.buildTrendsShareText(state)
        assertTrue(!result.contains("6 meses previos"), "Unexpected comparison in: $result")
    }

    @Test
    fun `buildTrendsShareText omits Mayores gastos section when topExpenses is empty`() {
        val state = ReportUiState(
            month = may2026,
            trends = TrendsUiData(topExpenses = emptyList()),
        )
        val result = ReportShareFormatter.buildTrendsShareText(state)
        assertTrue(!result.contains("Mayores gastos"), "Unexpected 'Mayores gastos' in: $result")
    }
}
