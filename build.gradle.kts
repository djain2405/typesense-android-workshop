plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.kotlin.jvm") version "2.0.21" apply false
}

tasks.register("indexSessions") {
    group = "workshop"
    description = "Create the Typesense collection and import data/sessions.json. Uses the admin key from local.properties."
    dependsOn(":tools:indexSessions")
}
