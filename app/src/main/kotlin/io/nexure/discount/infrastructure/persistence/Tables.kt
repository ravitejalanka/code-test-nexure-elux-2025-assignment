package io.nexure.discount.infrastructure.persistence

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object ProductTable : Table("products") {
    val id = varchar("id", 255)
    val name = varchar("name", 255)
    val basePrice = double("base_price")
    val country = varchar("country", 50)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object AppliedDiscountTable : Table("applied_discounts") {
    val productId = varchar("product_id", 255) references ProductTable.id
    val discountId = varchar("discount_id", 255)
    val percent = double("percent")
    val appliedAt = timestamp("applied_at")

    override val primaryKey = PrimaryKey(productId, discountId)

    init {
        uniqueIndex("unique_product_discount", productId, discountId)
    }
}