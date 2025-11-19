package io.nexure.discount.application.usecase

import arrow.core.*
import io.nexure.discount.domain.model.DiscountId
import io.nexure.discount.domain.model.DomainError
import io.nexure.discount.domain.model.Product
import io.nexure.discount.domain.model.ProductId
import io.nexure.discount.domain.model.ValidationError
import io.nexure.discount.domain.repository.ProductRepository
import io.nexure.discount.domain.service.ProductOperations.applyDiscount
import io.nexure.discount.domain.service.ProductValidation.validateDiscount

/**
 * Use case responsible for applying discounts to products with proper validation
 * and domain logic enforcement.
 *
 * This use case validates the discount parameters, applies business rules
 * through domain operations, and persists the results through the repository.
 *
 * @param repository The product repository for data persistence
 */
class ApplyDiscountUseCase(
    private val repository: ProductRepository
) {
    /**
     * Applies a discount to a product after validation and business rule enforcement.
     *
     * @param productId The unique identifier of the product to apply discount to
     * @param discountId The unique identifier of the discount (for idempotency)
     * @param percentValue The discount percentage (0-100)
     * @return Either an AppError if validation or business logic fails,
     *         or the updated Product with the new discount applied
     */
    suspend fun execute(
        productId: ProductId,
        discountId: DiscountId,
        percentValue: Double
    ): Either<AppError, Product> =
        validateDiscount(discountId, percentValue)
            .mapLeft { AppError.Validation(it) }
            .flatMap { discount ->
                repository
                    .findById(productId)
                    .mapLeft { AppError.Domain(it) }
                    .flatMap { product ->
                        // Use domain business logic first!
                        applyDiscount(product, discount)
                            .mapLeft { AppError.Domain(it) }
                            .flatMap { updatedProduct ->
                                // Then persist to database
                                repository.save(updatedProduct)
                                    .mapLeft { AppError.Domain(it) }
                                    .map { updatedProduct }
                            }
                    }
            }
}

/**
 * Application-level error types that wrap domain and validation errors.
 *
 * This sealed interface provides a clean separation between different
 * types of errors that can occur in the application layer.
 */
sealed interface AppError {
    /**
     * Validation errors that occur during input parameter validation.
     */
    data class Validation(val error: ValidationError) : AppError

    /**
     * Domain errors that occur during business logic execution.
     */
    data class Domain(val error: DomainError) : AppError
}