// A copy of BlueMapHUD's CoordinateShareEvent, compile-only. Never shipped: BlueMapHUD supplies the real class.
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
