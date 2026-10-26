plugins {
    application
    id("java")
    id("com.gradleup.shadow")
    id("org.openjfx.javafxplugin") version "0.1.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cps-core"))
    implementation("ch.qos.logback:logback-classic:1.6.3")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.testfx:openjfx-monocle:21.0.2")
    testImplementation("org.hamcrest:hamcrest:3.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.3.232")
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
