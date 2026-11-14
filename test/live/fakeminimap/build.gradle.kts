// Test only, for the live tests. Never shipped.
plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly(rootProject.extra["paperApi"] as String)
}

// Ships the same copy of the share event that Mayor compiles against.
sourceSets.main {
    java.srcDir(rootProject.file("test/stub/src/main/java"))
}

tasks.jar {
    archiveBaseName.set("LiveTestFakeMinimap")
}
