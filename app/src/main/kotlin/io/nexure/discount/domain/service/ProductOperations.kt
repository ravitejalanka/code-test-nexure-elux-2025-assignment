package io.nexure.discount.domain.service

import arrow.core.*
import io.nexure.discount.domain.model.*

object ProductOperations {

    fun hasDiscount(product: Product, discountId: DiscountId): Boolean =
        product.discounts.any { it.discountId == discountId }

    fun applyDiscount(
        product: Product,
        discount: AppliedDiscount
    ): Either<DomainError, Product> =
        if (hasDiscount(product, discount.discountId)) {
            DomainError.DiscountAlreadyApplied(product.id, discount.discountId).left()
        } else {
            product.copy(discounts = product.discounts + discount).right()
        }

    fun calculateFinalPrice(product: Product): Money {
        val totalDiscount = product.discounts.sumOf { it.percent.value }
        return product.basePrice
            .times(1 - totalDiscount / 100)
            .times(1 + product.country.vatPercent / 100)
    }

    fun getDiscounts(product: Product): List<AppliedDiscount> =
        product.discounts.toList()
}