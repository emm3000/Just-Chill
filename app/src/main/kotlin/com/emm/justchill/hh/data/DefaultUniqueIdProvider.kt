package com.emm.justchill.hh.data

import java.util.UUID

object DefaultUniqueIdProvider : com.emm.justchill.hh.data.UniqueIdProvider {

    override val uniqueId: String
        get() = UUID.randomUUID().toString()
}