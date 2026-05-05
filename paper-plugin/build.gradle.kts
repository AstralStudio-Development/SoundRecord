plugins {
    `java-library`
    id("com.gradleup.shadow")
}

val paperApiVersion = providers.gradleProperty("paper_api_version").get()

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")
    api(project(":common"))
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
