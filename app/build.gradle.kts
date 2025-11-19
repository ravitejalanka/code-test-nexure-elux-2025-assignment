plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    // Ktor server
    implementation(libs.bundles.ktor.server)

    // Arrow-kt for functional programming
    implementation(libs.bundles.arrow)

    // Database support
    implementation(libs.bundles.database)

    // Testing
    testImplementation(libs.bundles.testing)
    testImplementation(kotlin("test"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(22)
    }
}

application {
    mainClass = "io.nexure.discount.ApplicationKt"
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
}