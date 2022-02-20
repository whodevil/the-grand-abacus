plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
    id("org.owasp.dependencycheck") version "6.5.3"
    groovy
    application
    jacoco
}

repositories {
    mavenCentral()
}

val jacocoVersion = "0.8.7"

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.google.guava:guava:30.1.1-jre")
    implementation("com.google.api-client:google-api-client:1.33.0")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.apis:google-api-services-sheets:v4-rev20210629-1.32.1")
    implementation("com.google.inject:guice:4.2.0")
    implementation("dev.misfitlabs.kotlinguice4:kotlin-guice:1.4.0")
    implementation("io.github.microutils:kotlin-logging:1.7.6")
    implementation("ch.qos.logback:logback-classic:1.2.10")
    implementation("org.slf4j:jcl-over-slf4j:1.7.25")
    implementation("org.slf4j:jul-to-slf4j:1.7.25")
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("com.webcohesion.ofx4j:ofx4j:1.31")
    implementation("org.ccil.cowan.tagsoup:tagsoup:1.2")
    implementation("com.opencsv:opencsv:5.5.2")

    testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testImplementation("org.jacoco:org.jacoco.agent:${jacocoVersion}:runtime")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

dependencyCheck {
    failBuildOnCVSS = 2f
    cveValidForHours = 1
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

jacoco {
    toolVersion = jacocoVersion
}

application {
    // Define the main class for the application.
    mainClass.set("the.grand.abacus.AppKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions {
        jvmTarget = "11"
    }
}
