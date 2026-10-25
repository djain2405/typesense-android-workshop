import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.typesense:typesense-java:2.1.0")
    implementation("org.json:json:20240303")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks.register<JavaExec>("indexSessions") {
    group = "workshop"
    description = "Index data/books.json. The admin key is read here and is not packaged into the app."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.workshop.typesense.index.IndexWorkshopKt")
    workingDir = rootProject.projectDir
}
