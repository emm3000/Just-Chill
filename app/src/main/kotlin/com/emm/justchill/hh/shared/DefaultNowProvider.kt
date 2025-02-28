package com.emm.justchill.hh.shared

import java.time.Instant

object DefaultNowProvider : NowProvider {

    override val now: Long
        get() = Instant.now().toEpochMilli()
}