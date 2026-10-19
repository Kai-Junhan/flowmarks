plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.19.0"
}

group = "flowmarks.intellij"
version = "0.3.0"

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

val localIdeaPath = providers.gradleProperty("ideaPath")
    .orElse(providers.environmentVariable("FLOWMARKS_IDEA_PATH"))
    .orNull

dependencies {
    intellijPlatform {
        if (localIdeaPath.isNullOrBlank()) {
            intellijIdea("2025.3.6.1")
        } else {
            local(localIdeaPath)
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
    testRuntimeOnly("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "253"
            untilBuild = "253.*"
        }
    }
}

tasks {
    withType<JavaCompile>().configureEach {
        options.release = 21
        options.encoding = "UTF-8"
    }

    test {
        useJUnitPlatform()
    }
}
