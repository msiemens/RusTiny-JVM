import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val junitVersion = "5.7.0"

plugins {
    kotlin("jvm") version "1.4.10"
    application
    jacoco
}
group = "de.msiemens"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}
dependencies {
    implementation("org.ow2.asm:asm:9.0")
    implementation("org.ow2.asm:asm-util:9.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "13"
}
tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }

    finalizedBy(tasks.jacocoTestReport)
}
tasks.jacocoTestReport {
    dependsOn(tasks.test) // tests are required to run before generating the report
}

application {
    mainClass.set("de.msiemens.educomp.MainKt")
}
