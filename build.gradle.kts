plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

group = "io.uresolvr"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core reactive web
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.cache)

    // Data - R2DBC reactive
    implementation(libs.spring.boot.starter.data.r2dbc)
    implementation(libs.r2dbc.h2)
    runtimeOnly(libs.h2.database)

    // Flyway migrations (runs on JDBC, then app uses R2DBC)
    implementation(libs.flyway.core)
    runtimeOnly(libs.spring.jdbc)

    // Optional PostgreSQL (activate with 'postgres' profile)
    runtimeOnly(libs.r2dbc.postgresql)
    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.database.postgresql)

    // Security
    implementation(libs.spring.boot.starter.security)
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Crypto
    implementation(libs.bouncycastle.prov)

    // Cache
    implementation(libs.caffeine)

    // Observability
    implementation(libs.micrometer.prometheus)

    // API docs
    implementation(libs.springdoc.openapi.webflux)

    // Web UI templates
    implementation(libs.spring.boot.starter.thymeleaf)

    // Dev
    developmentOnly(libs.spring.boot.devtools)

    // Testing
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.reactor.test)
    testImplementation(libs.spring.security.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.bootJar {
    archiveFileName.set("uresolvr.jar")
}
