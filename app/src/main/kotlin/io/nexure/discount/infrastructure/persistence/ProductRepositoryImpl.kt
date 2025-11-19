package io.nexure.discount.infrastructure.persistence

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import io.nexure.discount.domain.model.AppliedDiscount
import io.nexure.discount.domain.model.Country
import io.nexure.discount.domain.model.DomainError
import io.nexure.discount.domain.model.DiscountId
import io.nexure.discount.domain.model.Money
import io.nexure.discount.domain.model.Percent
import io.nexure.discount.domain.model.Product
import io.nexure.discount.domain.model.ProductId
import io.nexure.discount.domain.model.ProductName
import io.nexure.discount.domain.repository.ProductRepository
import io.nexure.discount.domain.service.ProductOperations
import io.nexure.discount.domain.service.ProductValidation.validateCountry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * PostgreSQL implementation of ProductRepository using Exposed ORM.
 *
 * This repository provides database-enforced idempotency through unique constraints
 * on the applied_discounts table, ensuring that the same discount cannot be applied
 * twice to the same product even under concurrent load.
 *
 * @param database The configured Exposed Database instance
 */
class ProductRepositoryImpl(
    private val database: Database
) : ProductRepository {

    /**
 * Retrieves a product by its unique identifier including all applied discounts.
 *
 * @param id The unique identifier of the product to retrieve
 * @return Either a DomainError if the product is not found or database error occurs,
 *         or the Product with all its applied discounts
 */
override suspend fun findById(id: ProductId): Either<DomainError, Product> =
        Either.catch {
            newSuspendedTransaction(Dispatchers.IO, database) {
                ProductTable
                    .selectAll().where { ProductTable.id eq id.value }
                    .singleOrNull()
            }
        }
            .mapLeft { e -> DomainError.DatabaseError("Failed to query product", e) }
            .flatMap { row ->
                row?.let { loadProductWithDiscounts(it) }
                    ?: DomainError.ProductNotFound(id).left()
            }

    /**
 * Retrieves all products for a specific country including their applied discounts.
 *
 * @param country The country to filter products by
 * @return A Flow of Product entities for the specified country
 */
override fun findByCountry(country: Country): Flow<Product> = flow {
        Either.catch {
            newSuspendedTransaction(Dispatchers.IO, database) {
                ProductTable
                    .selectAll().where { ProductTable.country eq country.code }
                    .toList()
            }
        }
            .map { rows ->
                rows.forEach { row ->
                    loadProductWithDiscounts(row)
                        .onRight { product -> emit(product) }
                }
            }
    }

    /**
 * Saves a product and its applied discounts to the database.
 *
 * This method performs an upsert operation on products and attempts to insert
 * all associated discounts. Database unique constraints will prevent duplicate
 * discount applications, ensuring idempotency.
 *
 * @param product The product to save with all its applied discounts
 * @return Either a DomainError if save fails (including duplicate discount),
 *         or Unit on successful save
 */
override suspend fun save(product: Product): Either<DomainError, Unit> =
        Either.catch {
            newSuspendedTransaction(Dispatchers.IO, database) {
                // Check if product exists, if not insert
                val existingProduct = ProductTable
                    .selectAll().where { ProductTable.id eq product.id.value }
                    .singleOrNull()

                if (existingProduct == null) {
                    ProductTable.insert {
                        it[id] = product.id.value
                        it[name] = product.name.value
                        it[basePrice] = product.basePrice.amount
                        it[country] = product.country.code
                        it[createdAt] = java.time.Instant.now()
                    }
                }

                // Insert discounts (will fail if already exists due to unique constraint)
                ProductOperations.getDiscounts(product).forEach { discount ->
                    AppliedDiscountTable.insert {
                        it[productId] = product.id.value
                        it[discountId] = discount.discountId.value
                        it[percent] = discount.percent.value
                        it[appliedAt] = java.time.Instant.now()
                    }
                }
            }
        }.mapLeft { e ->
            // Database unique constraint violation (most common error for this operation)
            if (e is ExposedSQLException) {
                DomainError.DiscountAlreadyApplied(
                    product.id,
                    ProductOperations.getDiscounts(product).first().discountId
                )
            } else {
                DomainError.DatabaseError("Failed to save product", e)
            }
        }.map { }

    /**
 * Loads a product entity with all its applied discounts from a database row.
 *
 * This helper method constructs a complete Product object including all
 * associated discount records from the applied_discounts table.
 *
 * @param row The ResultRow containing the product data
 * @return Either a DomainError if loading fails, or the complete Product with discounts
 */
private fun loadProductWithDiscounts(row: ResultRow): Either<DomainError, Product> {
        val productId = ProductId(row[ProductTable.id])

        return Either.catch {
            val discounts = AppliedDiscountTable
                .selectAll().where { AppliedDiscountTable.productId eq productId.value }
                .map { discountRow ->
                    AppliedDiscount(
                        discountId = DiscountId(discountRow[AppliedDiscountTable.discountId]),
                        percent = Percent(discountRow[AppliedDiscountTable.percent])
                    )
                }
                .toSet()
            discounts
        }
            .mapLeft { e -> DomainError.DatabaseError("Failed to load discounts", e) }
            .flatMap { discounts ->
                validateCountry(row[ProductTable.country])
                    .mapLeft { DomainError.DatabaseError("Invalid country in database") }
                    .map { country ->
                        Product(
                            id = productId,
                            name = ProductName(row[ProductTable.name]),
                            basePrice = Money(row[ProductTable.basePrice]),
                            country = country,
                            discounts = discounts
                        )
                    }
            }
    }
}

