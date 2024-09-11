package com.emm.justchill.hh.data

import com.emm.justchill.hh.domain.NowProvider
import java.time.Instant

object DefaultNowProvider : NowProvider {

    override val now: Long
        get() = Instant.now().toEpochMilli()
}