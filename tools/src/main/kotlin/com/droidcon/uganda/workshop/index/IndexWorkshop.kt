package com.droidcon.uganda.workshop.index

import org.typesense.api.Client
import org.typesense.api.Configuration
import org.typesense.model.CollectionSchema
import org.typesense.api.FieldTypes
import org.typesense.model.Field
import org.typesense.model.ImportDocumentsParameters
import org.typesense.model.IndexAction
import org.typesense.resources.Node
import org.json.JSONArray
import java.io.File
import java.time.Duration
import java.util.Properties

/**
 * Run with ./gradlew indexSessions
 *
 * Exercise 1 is already implemented: it creates the collection and imports data/sessions.json.
 * Exercises 5 and 6 are the TODO functions at the bottom.
 */
fun main() {
    val properties = loadLocalProperties()
    val collection = properties.getProperty("typesense.collection")?.trim().orEmpty().ifBlank { "sessions" }
    val client = adminClient(properties)
    val indexed = exercise1CreateAndImport(client, collection)
    println("Indexed $indexed documents into '$collection'.")
    exercise5SynonymsAndCuration(client, collection)
    exercise6SearchOnlyKey(client, collection)
}

fun exercise1CreateAndImport(client: Client, collection: String): Int {
    val fields = listOf(
        Field().name("title").type(FieldTypes.STRING),
        Field().name("speaker").type(FieldTypes.STRING),
        Field().name("track").type(FieldTypes.STRING).facet(true),
        Field().name("description").type(FieldTypes.STRING),
        Field().name("place").type(FieldTypes.STRING).facet(true),
        Field().name("start_time").type(FieldTypes.INT64).sort(true),
        Field().name("location").type("geopoint")
    )
    val schema = CollectionSchema().name(collection).fields(fields).defaultSortingField("start_time")
    try {
        client.collections(collection).retrieve()
    } catch (_: Exception) {
        client.collections().create(schema)
    }
    return importSessions(client, collection)
}

private fun importSessions(client: Client, collection: String): Int {
    val sessions = parseSessions(File("data/sessions.json").readText())
    val jsonl = sessions.joinToString("\n")
    val parameters = ImportDocumentsParameters().action(IndexAction.UPSERT)
    client.collections(collection).documents().import_(jsonl, parameters)
    return client.collections(collection).retrieve().numDocuments.toInt()
}

/**
 * EXERCISE 5
 * Upsert a synonym set so `compose` also matches `jetpack`, and link it to this collection.
 * Upsert a curation set that pins document id `welcome` at position 1 when the query is `welcome`.
 *
 * Expect: search `compose` and the Jetpack talk appears.
 * Expect: search `welcome` and Welcome Address is the first result.
 */
fun exercise5SynonymsAndCuration(client: Client, collection: String) {
    println("EXERCISE 5 TODO: add the compose/jetpack synonym set and pin the welcome keynote.")
}

/**
 * EXERCISE 6
 * Create a key whose only action is documents:search on this collection.
 * Print the key once so it can be copied into typesense.searchApiKey.
 * Then try to import a document with that key and show that the write is rejected.
 */
fun exercise6SearchOnlyKey(client: Client, collection: String) {
    println("EXERCISE 6 TODO: create a search-only key and prove the app key cannot write.")
}

private fun adminClient(properties: Properties): Client {
    val protocol = properties.getProperty("typesense.protocol")?.trim().orEmpty().ifBlank { "http" }
    val host = properties.getProperty("typesense.host")?.trim().orEmpty()
    val port = properties.getProperty("typesense.port")?.trim().orEmpty().ifBlank { "8108" }
    val adminKey = properties.getProperty("typesense.adminApiKey")?.trim().orEmpty()
    require(host.isNotBlank()) { "Set typesense.host in local.properties" }
    require(adminKey.isNotBlank()) { "Set typesense.adminApiKey in local.properties" }
    val nodeHost = if (host == "10.0.2.2") "localhost" else host
    val node = Node(protocol, nodeHost, port)
    return Client(Configuration(listOf(node), Duration.ofSeconds(8), adminKey))
}

private fun loadLocalProperties(): Properties {
    val file = File("local.properties")
    require(file.exists()) { "Copy local.properties.example to local.properties" }
    return Properties().apply { file.inputStream().use { load(it) } }
}

private fun parseSessions(json: String): List<String> {
    val array = JSONArray(json)
    return List(array.length()) { index -> array.getJSONObject(index).toString() }
}
