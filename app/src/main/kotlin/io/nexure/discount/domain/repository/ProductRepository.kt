package io.nexure.discount.domain.repository

import arrow.core.Either
import io.nexure.discount.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    suspend fun findById(id: ProductId): Either<DomainError, Product>

    fun findByCountry(country: Country): Flow<Product>

    suspend fun save(product: Product): Either<DomainError, Unit>
}