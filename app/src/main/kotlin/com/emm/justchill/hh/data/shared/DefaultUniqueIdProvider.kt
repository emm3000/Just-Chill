package com.emm.justchill.hh.data.shared

import com.emm.justchill.hh.domain.shared.UniqueIdProvider
import java.util.UUID

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val uniqueId: String
        get() = UUID.randomUUID().toString()
}