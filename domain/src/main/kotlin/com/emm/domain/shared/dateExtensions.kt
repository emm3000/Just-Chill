package com.emm.domain.shared

import java.time.Instant

fun currentTimeInMillis(): Long = Instant.now().toEpochMilli()