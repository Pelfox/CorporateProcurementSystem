plugins {
    application
    `java-library`
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    implementation(project(":cps-core"))
    implementation(libs.jline)
    implementation(libs.logback)
    implementation(libs.hibernate.core)
    compileOnly(libs.jetbrains.annotations)
}
