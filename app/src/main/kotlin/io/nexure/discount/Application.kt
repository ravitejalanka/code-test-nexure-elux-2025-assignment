package io.nexure.discount

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.routing.*
import io.nexure.discount.application.usecase.ApplyDiscountUseCase
import io.nexure.discount.application.usecase.GetProductsByCountryUseCase
import io.nexure.discount.infrastructure.http.routes.productRoutes
import io.nexure.discount.infrastructure.persistence.ProductRepositoryImpl
import org.jetbrains.exposed.sql.Database

fun main() {
    embeddedServer(
        factory = Netty,
        port = 8082,
        host = "0.0.0.0",
        module = Application::module,
    ).start(true)
}

fun Application.module() {
    // Database setup
    val database = Database.connect(createHikariDataSource())
    val productRepository = ProductRepositoryImpl(database)

    // Use cases
    val getProductsUseCase = GetProductsByCountryUseCase(productRepository)
    val applyDiscountUseCase = ApplyDiscountUseCase(productRepository)

    // Configure plugins
    install(ContentNegotiation) {
        json()
    }

    // Configure routing
    routing {
        productRoutes(getProductsUseCase, applyDiscountUseCase)
    }
}

private fun createHikariDataSource(): HikariDataSource {
    val config = HikariConfig().apply {
        jdbcUrl = System.getenv("DB_URL") ?: "jdbc:postgresql://localhost:5432/nexure_products"
        username = System.getenv("DB_USER") ?: "nexure"
        password = System.getenv("DB_PASSWORD") ?: "nexure123"
        driverClassName = "org.postgresql.Driver"
        maximumPoolSize = 10
        minimumIdle = 5
    }
    return HikariDataSource(config)
}