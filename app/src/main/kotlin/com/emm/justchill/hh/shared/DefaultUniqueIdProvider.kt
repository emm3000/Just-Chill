package com.emm.justchill.hh.shared

import java.util.UUID

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val uniqueId: String
        get() = UUID.randomUUID().toString()
}