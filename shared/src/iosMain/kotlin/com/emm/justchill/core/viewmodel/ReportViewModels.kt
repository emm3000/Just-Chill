package com.emm.justchill.core.viewmodel

import com.emm.justchill.feature.report.ReportEffect
import com.emm.justchill.feature.report.ReportIntent
import com.emm.justchill.feature.report.ReportUiState
import com.emm.justchill.feature.report.ReportViewModel

fun resolveReportHandle(): MviHandle<ReportUiState, ReportIntent, ReportEffect> = handleOf(ReportViewModel::class)
