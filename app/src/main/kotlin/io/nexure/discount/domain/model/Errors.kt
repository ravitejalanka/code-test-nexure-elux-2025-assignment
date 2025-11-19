package io.nexure.discount.domain.model

sealed interface DomainError {
    data class ProductNotFound(val id: ProductId) : DomainError
    data class DiscountAlreadyApplied(
        val productId: ProductId,
        val discountId: DiscountId
    ) : DomainError
    data class DatabaseError(val message: String, val cause: Throwable? = null) : DomainError
}

sealed interface ValidationError {
    data object BlankProductName : ValidationError
    data class ProductNameTooLong(val length: Int) : ValidationError
    data class InvalidAmount(val value: Double) : ValidationError
    data class InvalidPercent(val value: Double) : ValidationError
    data class InvalidCountry(val value: String) : ValidationError
}