plugins {
    id("net.fabricmc.fabric-loom") version "1.16.0-alpha.13" apply false
    id("com.gradleup.shadow") version "8.3.6" apply false
}

allprojects {
    group = "cn.starry.soundrecord"
    version = "1.0.0"
}

subprojects {
    plugins.withId("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
        }
    }
}

tasks.register<Copy>("buildJars") {
    group = "build"
    description = "Builds the Fabric mod and Paper plugin jars, then copies them to dist."

    dependsOn(":fabric-mod:jar", ":paper-plugin:shadowJar")

    from(project(":fabric-mod").layout.buildDirectory.file("libs/fabric-mod-${project.version}.jar")) {
        rename { "SoundRecord-Fabric-${project.version}.jar" }
    }
    from(project(":paper-plugin").layout.buildDirectory.file("libs/paper-plugin-${project.version}.jar")) {
        rename { "SoundRecord-Paper-${project.version}.jar" }
    }
    into(layout.buildDirectory.dir("dist"))
}
