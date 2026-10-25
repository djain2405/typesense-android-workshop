import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun typesenseProperty(name: String, default: String = ""): String {
    return localProperties.getProperty(name)?.trim().orEmpty().ifBlank { default }
}

android {
    namespace = "com.workshop.typesense"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.workshop.typesense"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // The admin key is intentionally absent. Indexing reads it from the Gradle task.
        buildConfigField("String", "TYPESENSE_PROTOCOL", quote(typesenseProperty("typesense.protocol", "http")))
        buildConfigField("String", "TYPESENSE_HOST", quote(typesenseProperty("typesense.host")))
        buildConfigField("String", "TYPESENSE_PORT", quote(typesenseProperty("typesense.port", "8108")))
        buildConfigField("String", "TYPESENSE_SEARCH_API_KEY", quote(typesenseProperty("typesense.searchApiKey")))
        buildConfigField("String", "TYPESENSE_COLLECTION", quote(typesenseProperty("typesense.collection", "books")))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

fun quote(value: String): String = "\"" + value
    .replace("\\", "\\\\")
    .replace("\"", "\\\"") + "\""

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.typesense:typesense-java:2.1.0")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
