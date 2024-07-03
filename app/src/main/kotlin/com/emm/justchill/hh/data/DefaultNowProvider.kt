package com.emm.justchill.hh.data

import java.time.Instant

object DefaultNowProvider : com.emm.justchill.hh.data.NowProvider {

    override val now: Long
        get() = Instant.now().toEpochMilli()
}