package com.emm.justchill.hh.category

import com.emm.domain.category.CategoryRepository
import com.emm.domain.category.CategoryUpsert
import com.emm.domain.shared.SyncState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val now = 1736800000000L

val defaultCategories: List<CategoryUpsert>
    get() = listOf(

        // Food
        CategoryUpsert(
            categoryId = "c3c1d0a2-8f12-4b9e-9a36-1c4d2f0b2f01",
            name = "Groceries",
            icon = "groceries",
            color = "green",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "9d2a0c6e-9d3b-4a2e-9d71-1a3e8d5a6b02",
            name = "Restaurants",
            icon = "food",
            color = "orange",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "b1f7c9d4-3f2b-4f8c-a98e-6a7b8c9d0a03",
            name = "Fast Food",
            icon = "fast_food",
            color = "red",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "e2d9a7f3-1c3b-4c1a-b7a4-5c8d2f1a0b04",
            name = "Coffee",
            icon = "coffee",
            color = "brown",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "f1a9b2d3-6c4a-4d9a-9b2c-3f1a4d5e6f05",
            name = "Bar",
            icon = "bar",
            color = "purple",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),

        // Transport
        CategoryUpsert(
            categoryId = "a1b2c3d4-5f6a-4e9a-b1c2-3d4e5f6a7b06",
            name = "Transport",
            icon = "car",
            color = "blue",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "b2c3d4e5-6a7b-4c9d-a1b2-c3d4e5f607",
            name = "Taxi",
            icon = "taxi",
            color = "yellow",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "c3d4e5f6-7a8b-4d9e-b1c2-d3e4f5a608",
            name = "Public Transport",
            icon = "bus",
            color = "teal",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "d4e5f6a7-8b9c-4e1f-c2d3-e4f5a6b709",
            name = "Fuel",
            icon = "fuel",
            color = "orange",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "e5f6a7b8-9c1d-4f2e-d3e4-f5a6b7c80a",
            name = "Parking",
            icon = "parking",
            color = "gray",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),

        // Home
        CategoryUpsert(
            categoryId = "f6a7b8c9-1d2e-4a3b-e4f5-a6b7c8d90b",
            name = "Rent",
            icon = "rent",
            color = "purple",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "a7b8c9d1-2e3f-4b4c-f5a6-b7c8d9e10c",
            name = "Utilities",
            icon = "utilities",
            color = "yellow",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "b8c9d1e2-3f4a-4c5d-a6b7-c8d9e1f20d",
            name = "Internet",
            icon = "internet",
            color = "blue",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "c9d1e2f3-4a5b-4d6e-b7c8-d9e1f2a30e",
            name = "Home Repairs",
            icon = "repairs",
            color = "brown",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "d1e2f3a4-5b6c-4e7f-c8d9-e1f2a3b40f",
            name = "Cleaning",
            icon = "cleaning",
            color = "teal",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),

        // Income
        CategoryUpsert(
            categoryId = "e2f3a4b5-6c7d-4f8a-d9e1-f2a3b4c501",
            name = "Salary",
            icon = "salary",
            color = "green",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "f3a4b5c6-7d8e-4a9b-e1f2-a3b4c5d602",
            name = "Freelance",
            icon = "freelance",
            color = "blue",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "a4b5c6d7-8e9f-4b1c-f2a3-b4c5d6e703",
            name = "Investments",
            icon = "investment",
            color = "purple",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "b5c6d7e8-9f1a-4c2d-a3b4-c5d6e7f804",
            name = "Savings",
            icon = "savings",
            color = "teal",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        ),
        CategoryUpsert(
            categoryId = "c6d7e8f9-1a2b-4d3e-b4c5-d6e7f8a905",
            name = "Credit Card",
            icon = "credit_card",
            color = "red",
            syncState = SyncState.Synced,
            isDefault = true,
            updatedAt = now,
            createdAt = now
        )
    )


class CategorySeeder(private val repository: CategoryRepository) {

    suspend fun seed() = withContext(Dispatchers.IO) {
        val count = repository.count()
        if (count == 0L) {
            defaultCategories.forEach {
                repository.create(it)
            }
        }
    }
}