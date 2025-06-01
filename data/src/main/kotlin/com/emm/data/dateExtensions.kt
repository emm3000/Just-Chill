package com.emm.data

import java.time.Instant

fun currentTimeInMillis(): Long = Instant.now().toEpochMilli()