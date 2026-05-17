package com.emm.domain.shared

import kotlinx.datetime.Clock

fun currentTimeInMillis(): Long = Clock.System.now().toEpochMilliseconds()