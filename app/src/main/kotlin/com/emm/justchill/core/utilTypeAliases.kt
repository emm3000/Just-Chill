package com.emm.justchill.core

import kotlinx.coroutines.flow.Flow

typealias FlowResult<A> = Flow<Result<A>>