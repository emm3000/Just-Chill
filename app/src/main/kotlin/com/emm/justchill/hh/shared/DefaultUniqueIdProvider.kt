package com.emm.justchill.hh.shared

import com.emm.domain.shared.UniqueIdProvider
import java.util.UUID

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val id: String
        get() = UUID.randomUUID().toString()
}