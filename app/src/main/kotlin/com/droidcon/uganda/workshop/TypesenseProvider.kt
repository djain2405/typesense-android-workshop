package com.droidcon.uganda.workshop

import org.typesense.api.Client
import org.typesense.api.Configuration
import org.typesense.resources.Node
import java.time.Duration

fun workshopClient(): Client? {
    val host = BuildConfig.TYPESENSE_HOST
    val key = BuildConfig.TYPESENSE_SEARCH_API_KEY
    if (host.isBlank() || key.isBlank()) return null
    val node = Node(
        BuildConfig.TYPESENSE_PROTOCOL.ifBlank { "http" },
        host,
        BuildConfig.TYPESENSE_PORT.ifBlank { "8108" }
    )
    return Client(Configuration(listOf(node), Duration.ofSeconds(4), key))
}

fun workshopCollection(): String = BuildConfig.TYPESENSE_COLLECTION.ifBlank { "sessions" }
