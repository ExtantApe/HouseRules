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

tasks.jar {
    archiveBaseName.set("LiveTestHelper")
}
