package io.nexure.discount.infrastructure.http.routes

import arrow.core.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.nexure.discount.application.usecase.*
import io.nexure.discount.application.usecase.DiscountDTO
import io.nexure.discount.domain.model.*
import io.nexure.discount.domain.service.ProductOperations
import kotlinx.coroutines.flow.toList
import kotlinx.serialization.Serializable

/**
 * Defines HTTP routes for product-related operations.
 *
 * This function sets up routes for:
 * - GET /products?country={country} - Retrieve products by country
 * - PUT /products/{id}/discount - Apply discount to a product
 *
 * @param getProductsUseCase Use case for retrieving products by country
 * @param applyDiscountUseCase Use case for applying discounts to products
 */
fun Route.productRoutes(
    getProductsUseCase: GetProductsByCountryUseCase,
    applyDiscountUseCase: ApplyDiscountUseCase
) {
    route("/products") {
        get {
            call.request.queryParameters["country"]
                ?.let { countryCode ->
                    getProductsUseCase.execute(countryCode)
                        .fold(
                            ifLeft = { error -> call.respondAppError(error) },
                            ifRight = { productsFlow ->
                                productsFlow.toList()
                                    .let { products -> call.respond(HttpStatusCode.OK, products) }
                            }
                        )
                }
                ?: call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse("Country parameter is required")
                )
        }

        put("/{id}/discount") {
            parseApplyDiscountRequest(call)
                .flatMap { (productId, request) ->
                    applyDiscountUseCase.execute(
                        productId,
                        DiscountId(request.discountId),
                        request.percent
                    )
                }
                .map { product -> ProductResponse.from(product) }
                .fold(
                    ifLeft = { error -> call.respondAppError(error) },
                    ifRight = { response -> call.respond(HttpStatusCode.OK, response) }
                )
        }
    }
}

/**
 * Parses and validates the discount application request from HTTP call.
 *
 * @param call The Ktor ApplicationCall containing request data
 * @return Either an AppError if parsing/validation fails, or a Pair of ProductId and request data
 */
private suspend fun parseApplyDiscountRequest(
    call: ApplicationCall
): Either<AppError, Pair<ProductId, ApplyDiscountRequest>> =
    Either.catch {
        val productId = ProductId(
            call.parameters["id"] ?: throw IllegalArgumentException("Missing product id")
        )
        val request = call.receive<ApplyDiscountRequest>()
        productId to request
    }.mapLeft { e ->
        AppError.Validation(ValidationError.InvalidCountry("Invalid request format: ${e.message ?: "Parse error"}"))
    }

/**
 * Responds with appropriate HTTP error response based on application error.
 *
 * @param error The AppError to convert to HTTP response
 */
private suspend fun ApplicationCall.respondAppError(error: AppError) {
    error.toHttpResponse()
        .let { (status, message) ->
            respond(status, ErrorResponse(message))
        }
}

/**
 * Converts application errors to appropriate HTTP status codes and messages.
 *
 * @return Pair of HTTP status code and error message
 */
private fun AppError.toHttpResponse(): Pair<HttpStatusCode, String> = when (this) {
    is AppError.Validation -> when (error) {
        is ValidationError.InvalidCountry ->
            HttpStatusCode.BadRequest to "Invalid country: ${error.value}"
        is ValidationError.InvalidPercent ->
            HttpStatusCode.BadRequest to "Percent must be between 0 and 100: ${error.value}"
        is ValidationError.BlankProductName ->
            HttpStatusCode.BadRequest to "Product name cannot be blank"
        is ValidationError.ProductNameTooLong ->
            HttpStatusCode.BadRequest to "Product name too long: maximum 255 characters allowed"
        is ValidationError.InvalidAmount ->
            HttpStatusCode.BadRequest to "Amount must be positive: ${error.value}"
    }
    is AppError.Domain -> when (error) {
        is DomainError.ProductNotFound ->
            HttpStatusCode.NotFound to "Product not found: ${error.id.value}"
        is DomainError.DiscountAlreadyApplied ->
            HttpStatusCode.Conflict to "Discount ${error.discountId.value} already applied to product ${error.productId.value}"
        is DomainError.DatabaseError ->
            HttpStatusCode.InternalServerError to "Internal server error"
    }
}

/**
 * Request DTO for applying a discount to a product.
 *
 * @param discountId Unique identifier for the discount (used for idempotency)
 * @param percent Discount percentage value (0-100)
 */
@Serializable
data class ApplyDiscountRequest(
    val discountId: String,
    val percent: Double
)

/**
 * Error response DTO for API error responses.
 *
 * @param error Human-readable error message
 */
@Serializable
data class ErrorResponse(val error: String)

/**
 * Product response DTO with calculated final price.
 *
 * @param id Unique product identifier
 * @param name Product name
 * @param basePrice Original price before discounts and VAT
 * @param country Country code where product is sold
 * @param discounts List of applied discounts
 * @param finalPrice Calculated final price including VAT and discounts
 */
@Serializable
data class ProductResponse(
    val id: String,
    val name: String,
    val basePrice: Double,
    val country: String,
    val discounts: List<DiscountDTO>,
    val finalPrice: Double
) {
    companion object {
        /**
         * Creates a ProductResponse from a Product domain entity.
         *
         * @param product The Product domain entity to convert
         * @return ProductResponse with calculated final price
         */
        fun from(product: Product): ProductResponse = ProductResponse(
            id = product.id.value,
            name = product.name.value,
            basePrice = product.basePrice.amount,
            country = product.country.code,
            discounts = ProductOperations.getDiscounts(product).map {
                DiscountDTO(it.discountId.value, it.percent.value)
            },
            finalPrice = ProductOperations.calculateFinalPrice(product).amount
        )
    }
}