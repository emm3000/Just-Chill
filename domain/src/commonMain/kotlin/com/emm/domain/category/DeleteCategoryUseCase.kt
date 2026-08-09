package com.emm.domain.category

import com.emm.domain.shared.CategoryId

/**
 * Tombstones a category, and only that.
 *
 * This used to also null categoryId on every live transaction and recurring movement pointing at
 * the category, mirroring what SQL ON DELETE SET NULL would have done on a hard delete. Applied to
 * a soft delete, that rewrote history: a user removing a category they had stopped using also
 * erased the categorization of every past movement, permanently and across devices.
 *
 * Nothing is gained by it. Every read path joins categories with `c.deletedAt IS NULL`, so rows
 * pointing at a tombstoned category already render as "Sin categoría" whether the column is nulled
 * or not. Keeping the link costs nothing, survives the delete, and is what a future undo or
 * "restore category" would need. It also cuts the sync traffic of a delete from one row per
 * affected movement down to the single category tombstone.
 *
 * The one place that has to care is the backup export: a transaction may now carry a categoryId
 * whose category is tombstoned and therefore not in the exported category list, so the export
 * drops those dangling ids to keep the file self-consistent.
 */
class DeleteCategoryUseCase(private val repository: CategoryRepository) {

    suspend operator fun invoke(categoryId: CategoryId) = repository.delete(categoryId)
}
