package com.droidcon.uganda.workshop

import org.typesense.api.Client
import org.typesense.model.SearchParameters

/**
 * Exercises 2, 3, and 4 are the functions below this class.
 * Exercise 0 already calls them, so the screen updates as soon as you change a return value.
 */
class WorkshopSearch(
    private val client: Client,
    private val collection: String
) {
    fun search(query: String, track: String?, sortByTime: Boolean): WorkshopResult {
        val parameters = SearchParameters()
            .q(query.ifBlank { "*" })
            .queryBy(exercise2QueryBy())
            .prefix(exercise2Prefix())
            .perPage(30)

        exercise3Filter(track)?.let { parameters.filterBy(it) }
        exercise3Facet()?.let { parameters.facetBy(it) }
        exercise4HighlightFields()?.let { parameters.highlightFullFields(it) }
        exerciseSort(sortByTime)?.let { parameters.sortBy(it) }

        val result = client.collections(collection).documents().search(parameters)
        val hits = result.hits.orEmpty().map { hit ->
            val document = hit.document.orEmpty()
            SessionHit(
                id = document["id"].asText(),
                title = document["title"].asText(),
                speaker = document["speaker"].asText(),
                track = document["track"].asText(),
                place = document["place"].asText(),
                snippet = hit.highlights.orEmpty()
                    .firstOrNull { !it.snippet.isNullOrBlank() }
                    ?.snippet
                    .orEmpty()
            )
        }
        val facets = result.facetCounts.orEmpty()
            .firstOrNull { it.fieldName == "track" }
            ?.counts
            .orEmpty()
            .map { TrackFacet(name = it.value.orEmpty(), count = it.count ?: 0) }
        return WorkshopResult(hits = hits, facets = facets, found = result.found ?: hits.size)
    }
}

data class SessionHit(
    val id: String,
    val title: String,
    val speaker: String,
    val track: String,
    val place: String,
    val snippet: String
)

data class TrackFacet(val name: String, val count: Int)

data class WorkshopResult(
    val hits: List<SessionHit>,
    val facets: List<TrackFacet>,
    val found: Int
)

/**
 * EXERCISE 2
 * Search the title, the speaker name, and the track.
 * Then return "true" from [exercise2Prefix] so "kot" matches Kotlin before the word is finished.
 *
 * Expect: query_by is "title,speaker,track" and prefix is "true".
 * Type `kot` and the Kotlin talks show up.
 */
fun exercise2QueryBy(): String = "title"

fun exercise2Prefix(): String = "false"

/**
 * EXERCISE 3
 * When [track] is not "All", return a filter like `track:=Kotlin`.
 * Return "track" from [exercise3Facet] so each chip can show a count.
 *
 * Expect: tapping Kotlin leaves only the Kotlin talks, with a count on the chip.
 */
fun exercise3Filter(track: String?): String? = null

fun exercise3Facet(): String? = null

/**
 * EXERCISE 4
 * Highlight matches in the title and the description.
 *
 * Expect: searching `firebase` bolds the matched word under the Compose talk.
 */
fun exercise4HighlightFields(): String? = null

/**
 * Already wired. Relevance is the default. "By time" sends start_time:asc.
 */
fun exerciseSort(sortByTime: Boolean): String? = if (sortByTime) "start_time:asc" else null

private fun Any?.asText(): String = this?.toString().orEmpty()
