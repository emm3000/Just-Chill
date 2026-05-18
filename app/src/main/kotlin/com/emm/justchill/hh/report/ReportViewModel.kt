package com.emm.justchill.hh.report

import androidx.compose.ui.graphics.Color
import com.emm.domain.shared.YearMonth
import com.emm.domain.transaction.TransactionType
import com.emm.justchill.core.mvi.MviViewModel
import com.emm.justchill.core.theme.emmDarkColors
import kotlinx.datetime.Month
import java.text.NumberFormat
import java.util.Locale

/**
 * S1 implementation. Mock data hardcoded by (year, month, type).
 *
 * Replaces with real `GetMonthlyAmountByCategoryUseCase` +
 * `GetMonthlyComparisonUseCase` in S1 week 2-3 (slice vertical).
 *
 * For now: a deterministic mock keeps the UI honest while the
 * backend lands. Numbers vary per (year, month) so navigation
 * feels real.
 */
class ReportViewModel : MviViewModel<ReportUiState, ReportIntent, ReportEffect>() {

    override val initialState: ReportUiState =
        buildState(month = YearMonth.current(), type = TransactionType.Income)

    override fun onIntent(intent: ReportIntent) {
        when (intent) {
            ReportIntent.PreviousMonth -> updateState {
                buildState(month = month.previous(), type = selectedType)
            }
            ReportIntent.NextMonth -> updateState {
                buildState(month = month.next(), type = selectedType)
            }
            is ReportIntent.SelectType -> updateState {
                buildState(month = month, type = intent.type)
            }
        }
    }

    private fun buildState(month: YearMonth, type: TransactionType): ReportUiState {
        val shares = mockShares(month = month, type = type)
        val total = shares.sumOf { it.percentage * 100L } // pseudo-total in cents
        val realTotal = totalFor(month, type)
        val comparison = comparisonFor(month, type)

        return ReportUiState(
            month = month,
            selectedType = type,
            totalFormatted = formatSoles(realTotal),
            comparisonText = comparison?.let { (deltaPct, prevMonthLabel) ->
                val sign = if (deltaPct >= 0) "+" else ""
                "$sign$deltaPct% vs $prevMonthLabel"
            },
            comparisonIsPositive = comparison?.let { it.first >= 0 },
            shares = shares,
            isEmpty = shares.isEmpty() || realTotal == 0L,
        )
    }

    // ----------------- Mock data builders -----------------

    private fun mockShares(month: YearMonth, type: TransactionType): List<CategoryShare> {
        val seed = month.year * 12 + (month.month.ordinal + 1)
        // Stable variation so navigation feels real, but no rng.
        val drift = (seed % 5) - 2 // -2 .. 2

        return if (type == TransactionType.Income) {
            listOf(
                share("sueldo", "Sueldo", 4500 + drift * 50, 60 - drift, emmDarkColors.catSlate),
                share("freelance", "Freelance", 1200 + drift * 40, 19 + drift, emmDarkColors.catSage),
                share("ventas", "Ventas", 400 + drift * 20, 6, emmDarkColors.catTerracotta),
                share("propinas", "Propinas", 80, 1, emmDarkColors.catOchre),
                share("otros", "Otros", 20, 0, emmDarkColors.catGraphite),
            )
        } else {
            listOf(
                share("alquiler", "Alquiler", 1200, 50, emmDarkColors.catSlate),
                share("comida", "Comida", 600 + drift * 30, 25, emmDarkColors.catTerracotta),
                share("transporte", "Transporte", 300, 12, emmDarkColors.catSage),
                share("entretenimiento", "Entretenimiento", 200, 8, emmDarkColors.catMauve),
                share("servicios", "Servicios", 100, 5, emmDarkColors.catOchre),
            )
        }
    }

    private fun share(
        id: String,
        name: String,
        amountSoles: Int,
        percentage: Int,
        tint: Color,
    ): CategoryShare = CategoryShare(
        categoryId = id,
        name = name,
        amountFormatted = formatSoles(amountSoles.toLong()),
        percentage = percentage.coerceAtLeast(0),
        tint = tint,
    )

    private fun totalFor(month: YearMonth, type: TransactionType): Long {
        val seed = month.year * 12 + (month.month.ordinal + 1)
        val drift = (seed % 5) - 2
        return if (type == TransactionType.Income) (6200L + drift * 110)
        else (2400L + drift * 60)
    }

    private fun comparisonFor(
        month: YearMonth,
        type: TransactionType,
    ): Pair<Int, String>? {
        val current = totalFor(month, type)
        val previous = totalFor(month.previous(), type)
        if (previous == 0L) return null
        val delta = (((current - previous).toDouble() / previous) * 100).toInt()
        return delta to month.previous().shortLabel()
    }

    // ----------------- Format helpers -----------------

    private fun formatSoles(amount: Long): String {
        val nf = NumberFormat.getNumberInstance(Locale.forLanguageTag("es-PE"))
        nf.minimumFractionDigits = 0
        nf.maximumFractionDigits = 0
        return "S/ ${nf.format(amount)}"
    }
}

// ----------------- Extension on YearMonth for labels -----------------
// Lives in the same file for now — promote to shared utility if Home
// needs the same formatting.

fun YearMonth.fullLabel(): String = "${month.spanish()} $year"

fun YearMonth.shortLabel(): String = month.spanish()

private fun Month.spanish(): String = when (this) {
    Month.JANUARY -> "Enero"
    Month.FEBRUARY -> "Febrero"
    Month.MARCH -> "Marzo"
    Month.APRIL -> "Abril"
    Month.MAY -> "Mayo"
    Month.JUNE -> "Junio"
    Month.JULY -> "Julio"
    Month.AUGUST -> "Agosto"
    Month.SEPTEMBER -> "Setiembre"
    Month.OCTOBER -> "Octubre"
    Month.NOVEMBER -> "Noviembre"
    Month.DECEMBER -> "Diciembre"
}
