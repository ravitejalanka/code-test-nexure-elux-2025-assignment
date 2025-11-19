package io.nexure.discount.domain.model

sealed interface Country {
    val code: String
    val vatPercent: Double

    data object Sweden : Country {
        override val code = "SE"
        override val vatPercent = 25.0
    }

    data object Germany : Country {
        override val code = "DE"
        override val vatPercent = 19.0
    }

    data object France : Country {
        override val code = "FR"
        override val vatPercent = 20.0
    }
}