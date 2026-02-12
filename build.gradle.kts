import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
}

group = "com.ifmineai"
version = "1.2.9"
description = "IF MineAI - Minecraft Plugin Development"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("com.google.genai:google-genai:1.36.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-Xlint:deprecation")
}

tasks.processResources {
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.shadowJar {
    archiveClassifier.set("")
    relocate("com.google.genai", "com.ifmineai.libs.genai")
    relocate("com.google.gson", "com.ifmineai.libs.gson")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

// ビルド後にdist/フォルダへJARをコピー (蓄積)
val timestamp: String = LocalDateTime.now().format(
    DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
)

tasks.register<Copy>("copyToLocalDist") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("dist"))
    rename { "IFMineAI-${project.version}-${timestamp}.jar" }
}

// 最新版を固定名でもコピー (サーバーのpluginsに配置しやすい)
tasks.register<Copy>("copyLatest") {
    dependsOn(tasks.shadowJar)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(layout.projectDirectory.dir("dist"))
    rename { "IFMineAI-latest.jar" }
}

tasks.build {
    finalizedBy("copyToLocalDist", "copyLatest")
}
