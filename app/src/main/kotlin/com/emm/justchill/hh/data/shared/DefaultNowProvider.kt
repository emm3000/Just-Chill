package com.emm.justchill.hh.data.shared

import com.emm.justchill.hh.domain.shared.NowProvider
import java.time.Instant

object DefaultNowProvider : NowProvider {

    override val now: Long
        get() = Instant.now().toEpochMilli()
}