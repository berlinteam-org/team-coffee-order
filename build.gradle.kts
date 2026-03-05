import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.JavaExec

plugins {
    kotlin("jvm") version "2.3.0"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:3.4.0")
    implementation("io.ktor:ktor-server-netty:3.4.0")
    implementation("ch.qos.logback:logback-classic:1.5.12")
}

application {
    mainClass.set("MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    )
}

tasks.named<JavaExec>("run") {
    // Mark source and resources as inputs so --continuous can watch them
    inputs.files(fileTree("src/main/kotlin"))
    inputs.files(fileTree("src/main/resources"))
}
