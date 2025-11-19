package io.nexure.discount.domain.model

data class AppliedDiscount(
    val discountId: DiscountId,
    val percent: Percent
)

data class Product(
    val id: ProductId,
    val name: ProductName,
    val basePrice: Money,
    val country: Country,
    val discounts: Set<AppliedDiscount>
)