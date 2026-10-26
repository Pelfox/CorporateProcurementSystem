plugins {
    application
    id("java")
    id("com.gradleup.shadow")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":cps-core"))
    implementation("org.jline:jline:3.30.6")
    implementation("ch.qos.logback:logback-classic:1.6.3")
    implementation("org.hibernate.orm:hibernate-core:7.4.13.Final")
    compileOnly("org.jetbrains:annotations:26.1.0")
}
