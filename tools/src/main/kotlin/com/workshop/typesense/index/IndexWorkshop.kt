package com.workshop.typesense.index

import org.json.JSONArray
import org.typesense.api.Client
import org.typesense.api.Configuration
import org.typesense.api.FieldTypes
import org.typesense.model.ApiKeySchema
import org.typesense.model.CollectionSchema
import org.typesense.model.CollectionUpdateSchema
import org.typesense.model.CurationInclude
import org.typesense.model.CurationItemCreateSchema
import org.typesense.model.CurationRule
import org.typesense.model.CurationSetCreateSchema
import org.typesense.model.Field
import org.typesense.model.ImportDocumentsParameters
import org.typesense.model.IndexAction
import org.typesense.model.SynonymItemSchema
import org.typesense.model.SynonymSetCreateSchema
import org.typesense.resources.Node
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Properties

/**
 * Run with ./gradlew indexSessions
 *
 * Exercise 1 is already implemented: it creates the collection and imports data/books.json.
 * Exercises 5 and 6 are the TODO functions at the bottom.
 */
fun main() {
    val properties = loadLocalProperties()
    val collection = properties.getProperty("typesense.collection")?.trim().orEmpty().ifBlank { "books" }
    val client = adminClient(properties)
    val indexed = exercise1CreateAndImport(client, collection)
    println("Indexed $indexed documents into '$collection'.")
    exercise5SynonymsAndCuration(client, collection)
    exercise6SearchOnlyKey(client, collection)
}

fun exercise1CreateAndImport(client: Client, collection: String): Int {
    val fields = listOf(
        Field().name("title").type(FieldTypes.STRING),
        Field().name("authors").type(FieldTypes.STRING),
        Field().name("genre").type(FieldTypes.STRING).facet(true),
        Field().name("description").type(FieldTypes.STRING),
        Field().name("publication_year").type(FieldTypes.INT64),
        Field().name("rating").type(FieldTypes.FLOAT).sort(true),
        Field().name("store").type(FieldTypes.STRING).facet(true),
        Field().name("location").type("geopoint")
    )
    val schema = CollectionSchema().name(collection).fields(fields).defaultSortingField("rating")
    try {
        client.collections(collection).retrieve()
    } catch (_: Exception) {
        client.collections().create(schema)
    }
    return importBooks(client, collection)
}

private fun importBooks(client: Client, collection: String): Int {
    val books = parseBooks(File("data/books.json").readText())
    val jsonl = books.joinToString("\n")
    val parameters = ImportDocumentsParameters().action(IndexAction.UPSERT)
    client.collections(collection).documents().import_(jsonl, parameters)
    return client.collections(collection).retrieve().numDocuments.toInt()
}

/**
 * EXERCISE 5
 * Upsert a synonym set so `scifi` also matches `science fiction`, and link it to this collection.
 * Upsert a curation set that pins document id `pride` at position 1 when the query is exactly `classic`.
 *
 * Expect: search `scifi` and Dune, Neuromancer, and Foundation appear.
 * Expect: search `classic` and Pride and Prejudice is the first result.
 */
fun exercise5SynonymsAndCuration(client: Client, collection: String) {
    val synonyms = SynonymItemSchema()
        .id("scifi-words")
        .addSynonymsItem("scifi")
        .addSynonymsItem("science fiction")
    client.synonymSet("workshop-synonyms").upsert(
        SynonymSetCreateSchema().addItemsItem(synonyms)
    )
    client.collections(collection).update(
        CollectionUpdateSchema().addSynonymSetsItem("workshop-synonyms")
    )

    val pinPride = CurationItemCreateSchema()
        .id("pin-pride")
        .rule(CurationRule().query("classic").match(CurationRule.MatchEnum.EXACT))
        .addIncludesItem(CurationInclude().id("pride").position(1))
    client.curationSet("workshop-curation").upsert(
        CurationSetCreateSchema().addItemsItem(pinPride)
    )
    // typesense-java 2.1.0 can link synonym sets on update, but not curation sets.
    patchCollection(collection, """{"curation_sets":["workshop-curation"]}""")
    println("EXERCISE 5: scifi matches science fiction, and classic pins Pride and Prejudice.")
}

/**
 * EXERCISE 6
 * Create a key whose only action is documents:search on this collection.
 * Print the key once so it can be copied into typesense.searchApiKey.
 * Then try to import a document with that key and show that the write is rejected.
 */
fun exercise6SearchOnlyKey(client: Client, collection: String) {
    val created = client.keys().create(
        ApiKeySchema()
            .description("Workshop search only")
            .addActionsItem("documents:search")
            .addCollectionsItem(collection)
    )
    val searchKey = created.value
    println("EXERCISE 6 search-only key. Copy it into typesense.searchApiKey:")
    println(searchKey)

    val searchClient = clientWithKey(loadLocalProperties(), searchKey)
    try {
        searchClient.collections(collection).documents().create(
            mapOf("id" to "should-fail", "title" to "A write that must be rejected")
        )
        error("EXERCISE 6 failed: the search-only key was allowed to write.")
    } catch (exception: Exception) {
        println("EXERCISE 6 write rejected, as expected: ${exception.message}")
    }
}

private fun clientWithKey(properties: Properties, apiKey: String): Client {
    val protocol = properties.getProperty("typesense.protocol")?.trim().orEmpty().ifBlank { "http" }
    val host = properties.getProperty("typesense.host")?.trim().orEmpty()
    val port = properties.getProperty("typesense.port")?.trim().orEmpty().ifBlank { "8108" }
    val nodeHost = if (host == "10.0.2.2") "localhost" else host
    return Client(Configuration(listOf(Node(protocol, nodeHost, port)), Duration.ofSeconds(8), apiKey))
}

private fun patchCollection(collection: String, body: String) {
    val properties = loadLocalProperties()
    val protocol = properties.getProperty("typesense.protocol")?.trim().orEmpty().ifBlank { "http" }
    val host = properties.getProperty("typesense.host")?.trim().orEmpty()
    val port = properties.getProperty("typesense.port")?.trim().orEmpty().ifBlank { "8108" }
    val adminKey = properties.getProperty("typesense.adminApiKey")?.trim().orEmpty()
    val nodeHost = if (host == "10.0.2.2") "localhost" else host
    val request = HttpRequest.newBuilder(URI.create("$protocol://$nodeHost:$port/collections/$collection"))
        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
        .header("X-TYPESENSE-API-KEY", adminKey)
        .header("Content-Type", "application/json")
        .build()
    val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
    check(response.statusCode() in 200..299) {
        "Could not link the curation set (${response.statusCode()}): ${response.body()}"
    }
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

private fun parseBooks(json: String): List<String> {
    val array = JSONArray(json)
    return List(array.length()) { index -> array.getJSONObject(index).toString() }
}
