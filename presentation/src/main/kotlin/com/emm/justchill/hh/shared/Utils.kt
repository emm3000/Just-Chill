package com.emm.justchill.hh.shared

private val EmptyString: String = String()

val String.Companion.Empty: String
    get() = EmptyString
