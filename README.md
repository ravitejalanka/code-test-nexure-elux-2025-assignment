# Nexure Product Discount API

A Kotlin/Ktor API implementing product discount functionality with database-enforced idempotency using functional programming patterns with Arrow-kt.

## Features

- ✅ Functional programming with Arrow-kt Either railway
- ✅ Database-enforced idempotency (PostgreSQL unique constraint)
- ✅ Data-oriented programming (data separate from behavior)
- ✅ Concurrent-safe discount application
- ✅ Country-specific VAT calculations
- ✅ Clean architecture with domain separation

## Prerequisites

- Java 22
- Docker & Docker Compose
- Gradle (included via wrapper)

## Quick Start

### 1. Start Database

```bash
docker-compose up -d
```

### 2. Run Database Schema

```bash
docker exec -i nexure-postgres psql -U nexure -d nexure_products < schema.sql
```

### 3. Build & Run

```bash
./gradlew run
```

Application runs on: http://localhost:8082

## API Endpoints

### Get Products by Country

Returns all products for a country with calculated final prices including VAT and applied discounts.

```bash
curl "http://localhost:8082/products?country=SE"
```

**Response:**
```json
[
  {
    "id": "prod-1",
    "name": "Laptop",
    "basePrice": 1000.0,
    "country": "SE",
    "discounts": [
      {
        "discountId": "summer-sale",
        "percent": 10.0
      }
    ],
    "finalPrice": 825.0
  }
]
```

### Apply Discount

Applies a discount to a product idempotently. Same discount cannot be applied twice to the same product.

```bash
curl -X PUT "http://localhost:8082/products/prod-1/discount" \
  -H "Content-Type: application/json" \
  -d '{"discountId": "summer-sale", "percent": 10.0}'
```

**Success Response (200):**
```json
{
  "id": "prod-1",
  "name": "Laptop",
  "basePrice": 1000.0,
  "country": "SE",
  "discounts": [
    {
      "discountId": "summer-sale",
      "percent": 10.0
    }
  ],
  "finalPrice": 825.0
}
```

**Conflict Response (409) - Discount already applied:**
```json
{
  "error": "Discount summer-sale already applied to product prod-1"
}
```

## Business Rules

### Countries and VAT

- **Sweden (SE)**: 25% VAT
- **Germany (DE)**: 19% VAT
- **France (FR)**: 20% VAT

### Price Calculation

```
finalPrice = basePrice × (1 - totalDiscount%) × (1 + VAT%)
```

### Idempotency

- Same discount cannot be applied twice to the same product
- Database-level unique constraint enforces this under concurrent load
- Concurrent requests for the same discount result in 1 success, 99 conflicts

## Testing

```bash
./gradlew test
```

The test suite includes:
- Unit tests for domain logic
- Integration tests for database operations
- Concurrent tests verifying idempotency under load (100 simultaneous requests)

## Architecture

```
├── Domain Layer (Pure Business Logic)
│   ├── model/          # Data structures (Product, Country, etc.)
│   ├── service/        # Domain operations & validation
│   └── repository/     # Repository interfaces
├── Application Layer
│   └── usecase/        # Business use cases
└── Infrastructure Layer
    ├── persistence/    # Database implementation
    └── http/          # HTTP routes & DTOs
```

## Development

### Project Structure

```
src/main/kotlin/io/nexure/discount/
├── Application.kt                    # Main entry point
├── domain/
│   ├── model/
│   │   ├── Product.kt               # Product data structures
│   │   ├── ValueObjects.kt          # ProductId, Money, etc.
│   │   ├── Country.kt               # Country sealed interface
│   │   └── Errors.kt                # Domain & validation errors
│   ├── service/
│   │   ├── ProductValidation.kt     # Validation logic
│   │   └── ProductOperations.kt     # Business operations
│   └── repository/
│       └── ProductRepository.kt     # Repository interface
├── application/
│   └── usecase/
│       ├── ApplyDiscountUseCase.kt
│       └── GetProductsByCountryUseCase.kt
└── infrastructure/
    ├── persistence/
    │   ├── ExposedProductRepository.kt
    │   └── Tables.kt
    └── http/
        └── routes/
            └── ProductRoutes.kt
```

### Key Patterns

- **Functional Error Handling**: All operations return `Either<Error, Success>`
- **Data-Oriented**: Data structures are separate from behavior
- **Idempotency**: Database unique constraints prevent duplicate operations
- **Clean Architecture**: Dependency inversion with pure domain layer

## License

MIT License - see LICENSE file for details.