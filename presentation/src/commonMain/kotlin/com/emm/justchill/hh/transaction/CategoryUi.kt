package com.emm.justchill.hh.transaction

/**
 * Category as carried by presentation models: the domain's semantic ids, never resolved
 * Compose values. `iconId`/`colorId` are the plain strings the domain stores; the UI layer
 * resolves them at render time (see CategoryResolve.kt in :ui-android).
 */
data class CategoryUi(val iconId: String?, val colorId: String?)
