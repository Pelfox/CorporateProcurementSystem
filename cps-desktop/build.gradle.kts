plugins {
    application
    `java-library`
    id("java")
    alias(libs.plugins.shadow)
    alias(libs.plugins.javafxplugin)
}

dependencies {
    implementation(project(":cps-core"))
    api(libs.hibernate.core)
    implementation(libs.logback)
    testImplementation(libs.testfx.core)
    testImplementation(libs.testfx.junit5)
    testImplementation(libs.openjfx.monocle)
    testImplementation(libs.hamcrest)
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.h2database)
}

javafx {
    version = "25"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml"
    )
}

tasks.test {
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    systemProperty("testfx.robot", "glass")
    systemProperty("testfx.headless", "true")
    systemProperty("testfx.verbose", "true")
    failFast = false
    reports {
        junitXml.required.set(true)
        html.required.set(true)
    }
}
