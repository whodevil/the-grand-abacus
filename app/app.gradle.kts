plugins {
    groovy
    application
    jacoco
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.dependencycheck)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.guava)
    implementation(libs.bundles.google.cloud)
    implementation(libs.bundles.guice)
    implementation(libs.bundles.logging)
    implementation(libs.ofx4j)
    implementation(libs.tagsoup)
    implementation(libs.opencsv)
    implementation(libs.bundles.apache.commons)

    testImplementation(libs.bundles.spock)
    testImplementation("org.jacoco:org.jacoco.agent:${libs.versions.jacoco.get()}:runtime")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

allOpen {
    annotation("the.grand.abacus.OpenForTesting")
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
    toolVersion = libs.versions.jacoco.get()
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
