plugins {
    java
    id("com.gradleup.shadow") version "9.6.1"
}

group = "com.extantape"
version = "0.5.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

// Built against the oldest supported Paper, so nothing newer than 26.1 sneaks in.
val paperApi = "io.papermc.paper:paper-api:26.1.2.build.74-stable"
// Shared with the test-only plugins.
extra["paperApi"] = paperApi

dependencies {
    compileOnly(paperApi)
    // BlueMapMini's share event, for the optional hook. Supplied at runtime by BlueMapMini itself.
    compileOnly(project(":bluemapmini-stub"))
    // The shared command format (--json, describe), shaded in below.
    implementation("com.github.ExtantApe:Signalbox:v0.3.1")

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

allprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
        // Shows Paper API deprecations as they arrive, so they can be dealt with before removal.
        options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:removal"))
    }
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filesMatching("paper-plugin.yml") { expand(props) }
}

tasks.test {
    useJUnitPlatform()
}

// Signalbox goes inside the jar, relocated, so it can never clash with another plugin's copy. The shaded jar is the
// only jar built.
tasks.shadowJar {
    archiveBaseName.set("Mayor")
    archiveClassifier.set("")
    from("LICENSE")
    relocate("com.extantape.signalbox", "com.extantape.mayor.libs.signalbox")
}
tasks.jar { enabled = false }
tasks.assemble { dependsOn(tasks.shadowJar) }
