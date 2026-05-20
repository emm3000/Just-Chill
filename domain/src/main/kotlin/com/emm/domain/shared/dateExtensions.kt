package com.emm.domain.shared

import kotlin.time.Clock

fun currentTimeInMillis(): Long = Clock.System.now().toEpochMilliseconds()