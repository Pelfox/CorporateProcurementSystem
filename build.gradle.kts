plugins {
    application
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "com.team"
version = "1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains:annotations:26.1.0")
    implementation("ch.qos.logback:logback-classic:1.6.3")

    implementation("org.jline:jline:3.30.6")
    implementation("org.hibernate.orm:hibernate-core:7.4.8.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.zaxxer:HikariCP:7.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.3.232")

    implementation("org.sharegov:mjson:1.4.1")
    testImplementation("org.testfx:testfx-core:4.0.18")
    testImplementation("org.testfx:testfx-junit5:4.0.18")
    testImplementation("org.testfx:openjfx-monocle:21.0.2")
    testImplementation("org.hamcrest:hamcrest:3.0")
}

javafx {
    version = "25"
    modules = listOf(
        "javafx.controls",
        "javafx.fxml"
    )
}

application {
    mainClass = "com.team.corporate.MainApp"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

val javafxModuleNames = javafx.modules.joinToString(",")

tasks.withType<JavaExec>().configureEach {
    doFirst {
        // The JavaFX plugin configures only :run; IntelliJ creates its own JavaExec tasks.
        if (name == "run") {
            jvmArgs("--enable-native-access=javafx.graphics,ALL-UNNAMED")
        } else {
            val javafxJars = classpath.filter { it.name.startsWith("javafx-") && it.extension == "jar" }
            if (!javafxJars.isEmpty) {
                classpath = classpath.minus(javafxJars)
                jvmArgs(
                    "--module-path", javafxJars.asPath,
                    "--add-modules", javafxModuleNames,
                    "--enable-native-access=javafx.graphics,ALL-UNNAMED"
                )
            } else {
                jvmArgs("--enable-native-access=ALL-UNNAMED")
            }
        }
    }
}

tasks.withType<Jar>().configureEach {
    manifest.attributes["Enable-Native-Access"] = "ALL-UNNAMED"
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
