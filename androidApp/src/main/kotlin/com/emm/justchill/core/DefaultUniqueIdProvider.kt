@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.emm.justchill.core

import com.emm.justchill.core.domain.shared.UniqueIdProvider
import kotlin.uuid.Uuid

object DefaultUniqueIdProvider : UniqueIdProvider {

    override val id: String
        get() = Uuid.random().toString()
}
