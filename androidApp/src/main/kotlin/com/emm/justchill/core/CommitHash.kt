package com.emm.justchill.core

import kotlin.jvm.JvmInline

// A type, not a named("commitHash") qualifier: a qualifier string can drift between the producer
// and consumer modules and only fail with NoDefinitionFoundException at launch. Koin resolves this
// by KClass instead, so there is no string either side can misspell.
@JvmInline
value class CommitHash(val value: String)
