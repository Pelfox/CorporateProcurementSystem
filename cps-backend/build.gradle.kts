plugins {
    `java-library`
    id("java")
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
}

dependencies {
    implementation(project(":cps-core"))
    implementation(libs.spring.webmvc)
    implementation(libs.spring.security.starter)
    implementation(libs.spring.data.jpa.starter)

    compileOnly(libs.jetbrains.annotations)
    testImplementation(libs.spring.security.test)
}
