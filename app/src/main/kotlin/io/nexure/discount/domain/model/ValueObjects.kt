package io.nexure.discount.domain.model

@JvmInline
value class ProductId(val value: String)

@JvmInline
value class DiscountId(val value: String)

@JvmInline
value class ProductName(val value: String)

data class Money(val amount: Double) {
    operator fun times(factor: Double): Money = Money(amount * factor)
}

data class Percent(val value: Double)