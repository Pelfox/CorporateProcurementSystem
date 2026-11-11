plugins {
    `java-library`
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(libs.jetbrains.annotations)
    implementation(project(":cps-core"))
    implementation(libs.spring.webmvc)
    implementation(libs.spring.data.jpa)
    implementation(libs.spring.flyway)
    implementation(libs.spring.validation)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(libs.spring.webmvc.test)
    testRuntimeOnly(libs.h2database)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
