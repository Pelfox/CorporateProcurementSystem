plugins {
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

    implementation("org.hibernate.orm:hibernate-core:7.4.8.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.postgresql:postgresql:42.7.13")
    implementation("com.zaxxer:HikariCP:7.1.0")
}
