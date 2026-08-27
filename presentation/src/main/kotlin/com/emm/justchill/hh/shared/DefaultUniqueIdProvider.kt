@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emm.justchill.hh.shared

import com.emm.domain.shared.UniqueIdProvider
import kotlin.uuid.Uuid

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val id: String
        get() = Uuid.random().toString()
}
