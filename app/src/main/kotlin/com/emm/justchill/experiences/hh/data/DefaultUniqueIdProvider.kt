package com.emm.justchill.experiences.hh.data

import java.util.UUID

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val uniqueId: String
        get() = UUID.randomUUID().toString()
}