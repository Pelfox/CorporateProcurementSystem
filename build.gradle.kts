plugins {
    application
    id("java")
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.team"
version = "1.0-SNAPSHOT"

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
}

application {
    mainClass = "com.team.corporate.Main"
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaExec>().configureEach {
    standardInput = System.`in`
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<Jar>().configureEach {
    manifest.attributes["Enable-Native-Access"] = "ALL-UNNAMED"
}

tasks.test { useJUnitPlatform() }
