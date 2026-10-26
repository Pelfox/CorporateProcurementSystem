plugins {
    id("java")
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.hibernate.orm:hibernate-core:7.4.13.Final")
    implementation("jakarta.persistence:jakarta.persistence-api:3.2.0")
    implementation("org.postgresql:postgresql:42.7.14")
    implementation("com.zaxxer:HikariCP:7.1.0")

    compileOnly("org.jetbrains:annotations:26.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.3.232")
}
