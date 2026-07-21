plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
}

application {
    mainClass.set("com.prospectai.gateway.ApplicationKt")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content)
    implementation(libs.ktor.serialization)
    implementation(libs.ktor.server.status)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization)
    implementation(libs.logback)
    testImplementation(libs.ktor.server.test)
    testImplementation(libs.junit)
}
