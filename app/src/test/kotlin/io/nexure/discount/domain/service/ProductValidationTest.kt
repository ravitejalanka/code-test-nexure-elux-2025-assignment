package io.nexure.discount.domain.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.nexure.discount.domain.model.*

/**
 * Unit tests for ProductValidation to demonstrate functional error handling.
 *
 * This test shows clean unit testing without external dependencies.
 */
class ProductValidationTest : FunSpec({

    test("validateProductName should accept valid names") {
        val result = ProductValidation.validateProductName("Valid Product Name")

        result.isRight() shouldBe true
        result.getOrNull()?.value shouldBe "Valid Product Name"
    }

    test("validateProductName should reject blank names") {
        val result = ProductValidation.validateProductName("")

        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe ValidationError.BlankProductName
    }

    test("validateProductName should reject names that are too long") {
        val longName = "a".repeat(256)
        val result = ProductValidation.validateProductName(longName)

        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe ValidationError.ProductNameTooLong(256)
    }

    test("validateAmount should accept positive amounts") {
        val result = ProductValidation.validateAmount(100.0)

        result.isRight() shouldBe true
        result.getOrNull()?.amount shouldBe 100.0
    }

    test("validateAmount should reject zero or negative amounts") {
        val result = ProductValidation.validateAmount(0.0)

        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe ValidationError.InvalidAmount(0.0)
    }

    test("validatePercent should accept valid percentages") {
        val result = ProductValidation.validatePercent(50.0)

        result.isRight() shouldBe true
        result.getOrNull()?.value shouldBe 50.0
    }

    test("validatePercent should reject percentages out of range") {
        val result = ProductValidation.validatePercent(150.0)

        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe ValidationError.InvalidPercent(150.0)
    }

    test("validateCountry should accept valid countries") {
        val swedenResult = ProductValidation.validateCountry("SE")
        val germanyResult = ProductValidation.validateCountry("DE")
        val franceResult = ProductValidation.validateCountry("FR")

        swedenResult.isRight() shouldBe true
        swedenResult.getOrNull() shouldBe Country.Sweden

        germanyResult.isRight() shouldBe true
        germanyResult.getOrNull() shouldBe Country.Germany

        franceResult.isRight() shouldBe true
        franceResult.getOrNull() shouldBe Country.France
    }

    test("validateCountry should reject invalid countries") {
        val result = ProductValidation.validateCountry("InvalidCountry")

        result.isLeft() shouldBe true
        result.swap().getOrNull() shouldBe ValidationError.InvalidCountry("InvalidCountry")
    }

  test("validateCountry should reject full country names") {
        val swedenResult = ProductValidation.validateCountry("Sweden")
        val germanyResult = ProductValidation.validateCountry("Germany")
        val franceResult = ProductValidation.validateCountry("France")

        swedenResult.isLeft() shouldBe true
        swedenResult.swap().getOrNull() shouldBe ValidationError.InvalidCountry("Sweden")

        germanyResult.isLeft() shouldBe true
        germanyResult.swap().getOrNull() shouldBe ValidationError.InvalidCountry("Germany")

        franceResult.isLeft() shouldBe true
        franceResult.swap().getOrNull() shouldBe ValidationError.InvalidCountry("France")
    }

    test("validateDiscount should create valid AppliedDiscount") {
        val discountId = DiscountId("test-discount")
        val percent = 25.0

        val result = ProductValidation.validateDiscount(discountId, percent)

        result.isRight() shouldBe true
        val discount = result.getOrNull()
        discount?.discountId shouldBe discountId
        discount?.percent?.value shouldBe 25.0
    }
})