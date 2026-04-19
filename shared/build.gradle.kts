plugins {
    kotlin("jvm") version "2.2.21"
}

group = "com.glycin"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.springframework:spring-context:7.0.0")
    compileOnly("org.springframework.boot:spring-boot-autoconfigure:4.0.3")
    compileOnly("org.yaml:snakeyaml:2.4")
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(24)
}

tasks.test {
    useJUnitPlatform()
}