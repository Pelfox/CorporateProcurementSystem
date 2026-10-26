plugins {
    `java-library`
    id("java")
}

dependencies {
    api(libs.hibernate.core)
    implementation(libs.jakarta.persistence.api)
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    compileOnly(libs.jetbrains.annotations)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.h2database)
}
