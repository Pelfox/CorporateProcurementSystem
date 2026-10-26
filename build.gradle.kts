import org.gradle.api.plugins.JavaPluginExtension

plugins {
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.javafxplugin) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.spring.dependency.management) apply false
}

subprojects {
    val module = this

    group = "com.team.corporate"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    module.pluginManager.withPlugin("java-base") {
        module.extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(25))
            }
        }
    }
}
