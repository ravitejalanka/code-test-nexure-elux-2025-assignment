package io.nexure.discount.application.usecase

import arrow.core.*
import io.nexure.discount.domain.model.Product
import io.nexure.discount.domain.repository.ProductRepository
import io.nexure.discount.domain.service.ProductOperations
import io.nexure.discount.domain.service.ProductValidation.validateCountry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

/**
 * Use case responsible for retrieving products by country with calculated final prices.
 *
 * This use case validates the country parameter, retrieves products for that country,
 * and calculates final prices including VAT and applied discounts.
 *
 * @param repository The product repository for data retrieval
 */
class GetProductsByCountryUseCase(
    private val repository: ProductRepository
) {
    /**
     * Retrieves all products for a given country with calculated final prices.
     *
     * @param countryCode The country code to filter products by
     * @return Either an AppError if validation fails, or a Flow of ProductWithFinalPrice
     */
    fun execute(countryCode: String): Either<AppError, Flow<ProductWithFinalPrice>> =
       validateCountry(countryCode)
            .mapLeft { AppError.Validation(it) }
            .map { country ->
                repository
                    .findByCountry(country)
                    .map { product -> product.toProductWithFinalPrice() }
            }
}

private fun Product.toProductWithFinalPrice(): ProductWithFinalPrice =
    ProductWithFinalPrice(
        id = id.value,
        name = name.value,
        basePrice = basePrice.amount,
        country = country.code,
        discounts = ProductOperations.getDiscounts(this).map {
            DiscountDTO(it.discountId.value, it.percent.value)
        },
        finalPrice = ProductOperations.calculateFinalPrice(this).amount
    )

/**
 * Product data transfer object with calculated final price.
 *
 * @param id Unique product identifier
 * @param name Product name
 * @param basePrice Original price before discounts and VAT
 * @param country Country code where product is sold
 * @param discounts List of applied discounts
 * @param finalPrice Calculated final price including VAT and discounts
 */
@Serializable
data class ProductWithFinalPrice(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<DiscountDTO>,
    val finalPrice: Double
)

/**
 * Discount data transfer object for API responses.
 *
 * @param discountId Unique discount identifier
 * @param percent Discount percentage value
 */
@Serializable
data class DiscountDTO(
    val discountId: String,
    val percent: Double
)