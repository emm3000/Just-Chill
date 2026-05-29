package com.emm.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

fun provideSqlDriver(context: Context): SqlDriver = AndroidSqliteDriver(
    schema = EmmDatabaseData.Schema,
    context = context,
    name = "${BuildConfig.LIBRARY_PACKAGE_NAME}.db",
    callback = csm(),
)

fun csm() = object : AndroidSqliteDriver.Callback(schema = EmmDatabaseData.Schema) {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.setForeignKeyConstraintsEnabled(true)
    }

    // No onUpgrade override: AndroidSqliteDriver.Callback's default onUpgrade calls
    // EmmDatabaseData.Schema.migrate(driver, oldVersion, newVersion), which runs the
    // numbered .sqm files in order (e.g. 1.sqm: CREATE TABLE recurring_movements).
    // This preserves all existing user data across schema upgrades.

    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        seedDefaultCategories(db)
    }
}

private fun seedDefaultCategories(db: SupportSQLiteDatabase) {
    db.execSQL(
        """
        INSERT INTO categories (categoryId, name, icon, color, categoryType, isDefault, updatedAt, createdAt) VALUES
        ('c3c1d0a2-8f12-4b9e-9a36-1c4d2f0b2f01', 'Supermercado', 'groceries', 'green', 'Spend', 1, 1736800000000, 1736800000000),
        ('9d2a0c6e-9d3b-4a2e-9d71-1a3e8d5a6b02', 'Restaurantes', 'food', 'orange', 'Spend', 1, 1736800000000, 1736800000000),
        ('b1f7c9d4-3f2b-4f8c-a98e-6a7b8c9d0a03', 'Comida rápida', 'fast_food', 'red', 'Spend', 1, 1736800000000, 1736800000000),
        ('e2d9a7f3-1c3b-4c1a-b7a4-5c8d2f1a0b04', 'Café', 'coffee', 'brown', 'Spend', 1, 1736800000000, 1736800000000),
        ('f1a9b2d3-6c4a-4d9a-9b2c-3f1a4d5e6f05', 'Bar', 'bar', 'purple', 'Spend', 1, 1736800000000, 1736800000000),
        ('a1b2c3d4-5f6a-4e9a-b1c2-3d4e5f6a7b06', 'Transporte', 'car', 'blue', 'Spend', 1, 1736800000000, 1736800000000),
        ('b2c3d4e5-6a7b-4c9d-a1b2-c3d4e5f607', 'Taxi', 'taxi', 'yellow', 'Spend', 1, 1736800000000, 1736800000000),
        ('c3d4e5f6-7a8b-4d9e-b1c2-d3e4f5a608', 'Transporte público', 'bus', 'teal', 'Spend', 1, 1736800000000, 1736800000000),
        ('d4e5f6a7-8b9c-4e1f-c2d3-e4f5a6b709', 'Gasolina', 'fuel', 'orange', 'Spend', 1, 1736800000000, 1736800000000),
        ('e5f6a7b8-9c1d-4f2e-d3e4-f5a6b7c80a', 'Estacionamiento', 'parking', 'gray', 'Spend', 1, 1736800000000, 1736800000000),
        ('f6a7b8c9-1d2e-4a3b-e4f5-a6b7c8d90b', 'Alquiler', 'rent', 'purple', 'Spend', 1, 1736800000000, 1736800000000),
        ('a7b8c9d1-2e3f-4b4c-f5a6-b7c8d9e10c', 'Servicios básicos', 'utilities', 'yellow', 'Spend', 1, 1736800000000, 1736800000000),
        ('b8c9d1e2-3f4a-4c5d-a6b7-c8d9e1f20d', 'Internet', 'internet', 'blue', 'Spend', 1, 1736800000000, 1736800000000),
        ('c9d1e2f3-4a5b-4d6e-b7c8-d9e1f2a30e', 'Reparaciones del hogar', 'repairs', 'brown', 'Spend', 1, 1736800000000, 1736800000000),
        ('d1e2f3a4-5b6c-4e7f-c8d9-e1f2a3b40f', 'Limpieza', 'cleaning', 'teal', 'Spend', 1, 1736800000000, 1736800000000),
        ('e2f3a4b5-6c7d-4f8a-d9e1-f2a3b4c501', 'Sueldo', 'salary', 'green', 'Income', 1, 1736800000000, 1736800000000),
        ('f3a4b5c6-7d8e-4a9b-e1f2-a3b4c5d602', 'Freelance', 'freelance', 'blue', 'Income', 1, 1736800000000, 1736800000000),
        ('a4b5c6d7-8e9f-4b1c-f2a3-b4c5d6e703', 'Inversiones', 'investment', 'purple', 'Income', 1, 1736800000000, 1736800000000),
        ('b5c6d7e8-9f1a-4c2d-a3b4-c5d6e7f804', 'Ahorros', 'savings', 'teal', 'Income', 1, 1736800000000, 1736800000000),
        ('08aa808c-0165-4bdc-a15d-8b49a073dee0', 'Ventas', 'shopping', 'blue', 'Income', 1, 1736800000000, 1736800000000),
        ('b9625963-dbb7-4c76-8393-e0bd15f12b3c', 'Propinas', 'tips', 'yellow', 'Income', 1, 1736800000000, 1736800000000),
        ('ee8698c7-5c11-419d-a6bb-0242e931c6b0', 'Otros', 'wallet', 'gray', 'Income', 1, 1736800000000, 1736800000000),
        ('c6d7e8f9-1a2b-4d3e-b4c5-d6e7f8a905', 'Tarjeta de crédito', 'credit_card', 'red', 'Spend', 1, 1736800000000, 1736800000000);
        """.trimIndent(),
    )
}

fun provideDb(sqlDriver: SqlDriver): EmmDatabaseData = EmmDatabaseData(sqlDriver)

fun provideTransactionQueries(db: EmmDatabaseData): TransactionsQueries = db.transactionsQueries

fun provideRecurringMovementQueries(db: EmmDatabaseData): Recurring_movementsQueries = db.recurring_movementsQueries
