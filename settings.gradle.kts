rootProject.name = "the-grand-abacus"

val gprUsername = extra.has("gpr.user").let { if (it) extra.get("gpr.user") as String else System.getenv("USERNAME") as String}
val gprToken = extra.has("gpr.key").let { if (it) extra.get("gpr.key") as String else System.getenv("GITHUB_TOKEN") as String}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
        maven {
            url = uri("https://maven.pkg.github.com/whodevil/jvm-platform")
            credentials {
                username = gprUsername
                password = gprToken
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from("info.offthecob.jvm.platform:catalog:v0.0.10")

            library("ofx4j","com.webcohesion.ofx4j:ofx4j:1.31")
            library("tagsoup","org.ccil.cowan.tagsoup:tagsoup:1.2")
            library("opencsv", "com.opencsv:opencsv:5.5.2")

            library("google-api-client","com.google.api-client:google-api-client:1.33.0")
            library("google-oauth","com.google.oauth-client:google-oauth-client-jetty:1.32.1")
            library("google-sheets","com.google.apis:google-api-services-sheets:v4-rev20210629-1.32.1")
            bundle("google-cloud", listOf("google-api-client", "google-oauth", "google-sheets"))
        }
    }
}

rootProject.projectDir.listFiles().filter { it.isDirectory }.map { subDir ->
    subDir.listFiles().filter {
        it.isFile && it.name.contains(".gradle.kts")
    }.map {
        File(it.parent).name
    }
}.flatten().forEach {
    include(it)
}

rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
    assert(project.buildFile.isFile)
}