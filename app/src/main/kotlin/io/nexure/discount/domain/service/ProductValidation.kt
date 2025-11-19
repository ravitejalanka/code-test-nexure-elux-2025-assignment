package io.nexure.discount.domain.service

import arrow.core.*
import io.nexure.discount.domain.model.*

object ProductValidation {

    fun validateProductName(value: String): Either<ValidationError, ProductName> =
        value.takeIf { it.isNotBlank() }
            ?.takeIf { it.length <= 255 }
            ?.let { ProductName(it).right() }
            ?: when {
                value.isBlank() -> ValidationError.BlankProductName.left()
                else -> ValidationError.ProductNameTooLong(value.length).left()
            }

    fun validateAmount(value: Double): Either<ValidationError, Money> =
        if (value > 0) Money(value).right() else ValidationError.InvalidAmount(value).left()

    fun validatePercent(value: Double): Either<ValidationError, Percent> =
        if (value in 0.0..100.0) Percent(value).right() else ValidationError.InvalidPercent(value).left()

    fun validateCountry(value: String): Either<ValidationError, Country> =
        when (value.uppercase()) {
            "SE" -> Country.Sweden
            "DE" -> Country.Germany
            "FR" -> Country.France
            else -> null
        }?.right() ?: ValidationError.InvalidCountry(value).left()

    fun validateDiscount(
        discountId: DiscountId,
        percentValue: Double
    ): Either<ValidationError, AppliedDiscount> =
        validatePercent(percentValue).map { percent ->
            AppliedDiscount(discountId, percent)
        }
}