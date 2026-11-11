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
    implementation(libs.spring.security)
    implementation(libs.spring.resource.server)
    implementation(libs.spring.flyway)
    implementation(libs.spring.validation)

    runtimeOnly(libs.postgresql)
    runtimeOnly(libs.flyway.postgresql)

    testImplementation(libs.spring.webmvc.test)
    testImplementation(libs.spring.security.test)
    testRuntimeOnly(libs.h2database)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    exclude("**/*PostgresTest.class")
}

tasks.register<Test>("postgresTest") {
    description = "Checks authentication, Flyway and a packaged backend restart against a dedicated PostgreSQL test database."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    include("**/*PostgresTest.class")
    // External database state can change independently of source files or the JAR.
    outputs.upToDateWhen { false }
    val backendJar = tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar")
    dependsOn(backendJar)
    inputs.file(backendJar.flatMap { it.archiveFile })
    doFirst {
        check(!System.getenv("POSTGRES_TEST_URL").isNullOrBlank()) {
            "Set POSTGRES_TEST_URL to a dedicated, disposable PostgreSQL test database."
        }
        systemProperty("cps.backend.jar", backendJar.get().archiveFile.get().asFile.absolutePath)
    }
}
