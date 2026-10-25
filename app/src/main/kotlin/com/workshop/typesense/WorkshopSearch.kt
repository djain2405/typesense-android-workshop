package com.workshop.typesense

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
    fun search(query: String, genre: String?, sortByRating: Boolean): WorkshopResult {
        val parameters = SearchParameters()
            .q(query.ifBlank { "*" })
            .queryBy(exercise2QueryBy())
            .prefix(exercise2Prefix())
            .perPage(30)

        exercise3Filter(genre)?.let { parameters.filterBy(it) }
        exercise3Facet()?.let { parameters.facetBy(it) }
        exercise4HighlightFields()?.let { parameters.highlightFullFields(it) }
        exerciseSort(sortByRating)?.let { parameters.sortBy(it) }

        val result = client.collections(collection).documents().search(parameters)
        val hits = result.hits.orEmpty().map { hit ->
            val document = hit.document.orEmpty()
            BookHit(
                id = document["id"].asText(),
                title = document["title"].asText(),
                authors = document["authors"].asText(),
                genre = document["genre"].asText(),
                rating = document["rating"].asText(),
                store = document["store"].asText(),
                snippet = hit.highlights.orEmpty()
                    .firstOrNull { !it.snippet.isNullOrBlank() }
                    ?.snippet
                    .orEmpty()
            )
        }
        val facets = result.facetCounts.orEmpty()
            .firstOrNull { it.fieldName == "genre" }
            ?.counts
            .orEmpty()
            .map { GenreFacet(name = it.value.orEmpty(), count = it.count ?: 0) }
        return WorkshopResult(hits = hits, facets = facets, found = result.found ?: hits.size)
    }
}

data class BookHit(
    val id: String,
    val title: String,
    val authors: String,
    val genre: String,
    val rating: String,
    val store: String,
    val snippet: String
)

data class GenreFacet(val name: String, val count: Int)

data class WorkshopResult(
    val hits: List<BookHit>,
    val facets: List<GenreFacet>,
    val found: Int
)

/**
 * EXERCISE 2
 * Search the title, the author names, and the genre.
 * Then return "true" from [exercise2Prefix] so "har" matches Harry Potter before the word is finished.
 *
 * Expect: query_by is "title,authors,genre" and prefix is "true".
 * Type `har` and Harry Potter shows up.
 */
fun exercise2QueryBy(): String = "title,authors"

fun exercise2Prefix(): String = "false"

/**
 * EXERCISE 3
 * When [genre] is not "All", return a filter like `genre:=Fantasy`.
 * Return "genre" from [exercise3Facet] so each chip can show a count.
 *
 * Expect: tapping Fantasy leaves only the fantasy books, with a count on the chip.
 */
fun exercise3Filter(genre: String?): String? = null

fun exercise3Facet(): String? = null

/**
 * EXERCISE 4
 * Highlight matches in the title and the description.
 *
 * Expect: searching `arrakis` bolds the matched word under Dune.
 */
fun exercise4HighlightFields(): String? = null

/**
 * Already wired. Relevance is the default. "By rating" sends rating:desc.
 */
fun exerciseSort(sortByRating: Boolean): String? = if (sortByRating) "rating:desc" else null

private fun Any?.asText(): String = this?.toString().orEmpty()
