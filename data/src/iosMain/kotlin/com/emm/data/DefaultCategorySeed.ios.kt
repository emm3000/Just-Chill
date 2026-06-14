package com.emm.data

// iOS equivalent of DatabaseDriver.android.kt's seedDefaultCategories(db) which runs once
// inside AndroidSqliteDriver.Callback.onCreate. NativeSqliteDriver (SQLDelight 2.x) has NO
// onCreate hook (schema.create is wired internally and overriding onConfiguration would clobber
// it), so we seed AFTER the DB is built, guarded by an idempotent count check.
//
// The 23 default categories MUST stay byte-for-byte identical to the Android seed
// (same categoryId UUIDs, names, icons, colors, types, isDefault=1, fixed createdAt/updatedAt
// 1736800000000) so a future cross-device sync (phase 6) sees the same rows on both platforms.
// Do NOT diverge this set from data/src/androidMain/.../DatabaseDriver.android.kt.
//
// Idempotent: only inserts when countDefaultCategories() == 0, so it runs once on first launch
// and never clobbers existing user data (including user-created categories) on subsequent opens.
fun seedDefaultCategoriesIfEmpty(db: EmmDatabaseData) {
    val cq = db.categoriesQueries
    val existingDefaults = cq.countDefaultCategories().executeAsOne()
    if (existingDefaults > 0L) return

    val ts = 1736800000000L
    DEFAULT_CATEGORIES.forEach { c ->
        cq.insert(
            categoryId = c.id,
            name = c.name,
            icon = c.icon,
            color = c.color,
            categoryType = c.type,
            isDefault = true,
            updatedAt = ts,
            createdAt = ts,
        )
    }
}

private data class DefaultCategory(
    val id: String,
    val name: String,
    val icon: String,
    val color: String,
    val type: String,
)

// Mirrors DatabaseDriver.android.kt seedDefaultCategories INSERT, in the same order.
private val DEFAULT_CATEGORIES: List<DefaultCategory> = listOf(
    DefaultCategory("c3c1d0a2-8f12-4b9e-9a36-1c4d2f0b2f01", "Supermercado", "groceries", "green", "Spend"),
    DefaultCategory("9d2a0c6e-9d3b-4a2e-9d71-1a3e8d5a6b02", "Restaurantes", "food", "orange", "Spend"),
    DefaultCategory("b1f7c9d4-3f2b-4f8c-a98e-6a7b8c9d0a03", "Comida rápida", "fast_food", "red", "Spend"),
    DefaultCategory("e2d9a7f3-1c3b-4c1a-b7a4-5c8d2f1a0b04", "Café", "coffee", "brown", "Spend"),
    DefaultCategory("f1a9b2d3-6c4a-4d9a-9b2c-3f1a4d5e6f05", "Bar", "bar", "purple", "Spend"),
    DefaultCategory("a1b2c3d4-5f6a-4e9a-b1c2-3d4e5f6a7b06", "Transporte", "car", "blue", "Spend"),
    DefaultCategory("b2c3d4e5-6a7b-4c9d-a1b2-c3d4e5f607", "Taxi", "taxi", "yellow", "Spend"),
    DefaultCategory("c3d4e5f6-7a8b-4d9e-b1c2-d3e4f5a608", "Transporte público", "bus", "teal", "Spend"),
    DefaultCategory("d4e5f6a7-8b9c-4e1f-c2d3-e4f5a6b709", "Gasolina", "fuel", "orange", "Spend"),
    DefaultCategory("e5f6a7b8-9c1d-4f2e-d3e4-f5a6b7c80a", "Estacionamiento", "parking", "gray", "Spend"),
    DefaultCategory("f6a7b8c9-1d2e-4a3b-e4f5-a6b7c8d90b", "Alquiler", "rent", "purple", "Spend"),
    DefaultCategory("a7b8c9d1-2e3f-4b4c-f5a6-b7c8d9e10c", "Servicios básicos", "utilities", "yellow", "Spend"),
    DefaultCategory("b8c9d1e2-3f4a-4c5d-a6b7-c8d9e1f20d", "Internet", "internet", "blue", "Spend"),
    DefaultCategory("c9d1e2f3-4a5b-4d6e-b7c8-d9e1f2a30e", "Reparaciones del hogar", "repairs", "brown", "Spend"),
    DefaultCategory("d1e2f3a4-5b6c-4e7f-c8d9-e1f2a3b40f", "Limpieza", "cleaning", "teal", "Spend"),
    DefaultCategory("e2f3a4b5-6c7d-4f8a-d9e1-f2a3b4c501", "Sueldo", "salary", "green", "Income"),
    DefaultCategory("f3a4b5c6-7d8e-4a9b-e1f2-a3b4c5d602", "Freelance", "freelance", "blue", "Income"),
    DefaultCategory("a4b5c6d7-8e9f-4b1c-f2a3-b4c5d6e703", "Inversiones", "investment", "purple", "Income"),
    DefaultCategory("b5c6d7e8-9f1a-4c2d-a3b4-c5d6e7f804", "Ahorros", "savings", "teal", "Income"),
    DefaultCategory("08aa808c-0165-4bdc-a15d-8b49a073dee0", "Ventas", "shopping", "blue", "Income"),
    DefaultCategory("b9625963-dbb7-4c76-8393-e0bd15f12b3c", "Propinas", "tips", "yellow", "Income"),
    DefaultCategory("ee8698c7-5c11-419d-a6bb-0242e931c6b0", "Otros", "wallet", "gray", "Income"),
    DefaultCategory("c6d7e8f9-1a2b-4d3e-b4c5-d6e7f8a905", "Tarjeta de crédito", "credit_card", "red", "Spend"),
)
