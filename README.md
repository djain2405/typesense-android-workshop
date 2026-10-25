# Typesense workshop

A two-hour Android workshop. You start with a working bookstore search, then turn on the Typesense features that change the screen: typo tolerance, prefix search, filters, facets, highlights, synonyms, curation, and a search-only API key.

This is a native Android app: Kotlin, Jetpack Compose, and the Typesense Java client. It is not a Kotlin Multiplatform project.

Typesense is one C++ binary. It keeps the index in memory and answers typo-tolerant keyword search, plus vector search, in a single HTTP API. You do not need a separate keyword host and a separate vector database.

The phone app only receives the host and a search key. The admin key stays in `local.properties` and is used by `./gradlew indexSessions`.

## Before you sit down

1. Android Studio, an emulator (API 26 or newer), and JDK 17.
2. Copy `local.properties.example` to `local.properties`.
3. Set `sdk.dir` to your Android SDK.
4. Pick one connection:

| Where you run Typesense | `typesense.host` | `typesense.port` | `typesense.protocol` |
| --- | --- | --- | --- |
| Docker, Android emulator | `10.0.2.2` | `8108` | `http` |
| Docker, physical phone | your laptop's LAN IP | `8108` | `http` |
| Shared workshop cluster | the cluster hostname, without `https://` | `443` | `https` |

On a shared cluster, set `typesense.collection` to `books-` plus your name so you do not overwrite anyone else's index. Put the search-only key in `typesense.searchApiKey`. Keep the admin key in `typesense.adminApiKey`. The app never reads the admin key.

5. Local server, if you are not using the shared cluster:

```bash
docker compose up -d
./gradlew indexSessions
```

The index task talks to `localhost` when the app host is `10.0.2.2`, because Gradle runs on the laptop and the emulator address is only for the phone.

6. Run the `app` configuration. The first search runs as soon as the screen opens.

## Exercise 0: it already searches

No code changes.

- Type `tolkein`. You should see The Hobbit and The Lord of the Rings.
- Type `Rowling`. You should see Harry Potter and the Sorcerer's Stone.

`tolkein` is one typo away from `Tolkien`. Typesense allows that by default: one typo from 4 characters, two typos from 7. Author search already works because `query_by` includes `authors`.

If this search fails, fix the connection before you change any code. The instructor guide has the fallback.

## Exercise 1: create the collection

Open [`IndexWorkshop.kt`](tools/src/main/kotlin/com/workshop/typesense/index/IndexWorkshop.kt) and read `exercise1CreateAndImport`. Then run:

```bash
./gradlew indexSessions
```

You should see `Indexed 10 documents into 'books'.` The task creates the collection if it is missing and upserts `data/books.json`. Run it again. The count stays 10.

The JSON file is the source of truth. Typesense is only the search index.

## Exercise 2: query_by and prefix

Open [`WorkshopSearch.kt`](app/src/main/kotlin/com/workshop/typesense/WorkshopSearch.kt).

- Change `exercise2QueryBy` so it returns `title,authors,genre`.
- Change `exercise2Prefix` so it returns `true`.

Rebuild and type `har`. Harry Potter appears before you finish the word. Type `fantasy`. The fantasy books appear, because genre is now a searched field.

## Exercise 3: filter and facets

Still in `WorkshopSearch.kt`.

- `exercise3Filter`: when `genre` is not null and not `All`, return `genre:=` plus the genre name. Example: `genre:=Fantasy`.
- `exercise3Facet`: return `genre`.

Rebuild. The chips gain counts. Tap Fantasy and only the fantasy books remain. Tap All and the filter goes away.

## Exercise 4: highlights

`exercise4HighlightFields` should return `title,description`.

Rebuild and search `arrakis`. The matched word is highlighted on Dune.

Relevance stays the default. Tap **By rating** to send `rating:desc`. That parameter is already wired in `exerciseSort`.

## Exercise 5: synonyms and curation

Back in `IndexWorkshop.kt`, fill in `exercise5SynonymsAndCuration`.

1. Upsert a synonym set named `workshop-synonyms` with one multi-way item, id `scifi-words`, synonyms `scifi` and `science fiction`.
2. Link that set to the collection with `collections(name).update(...)` and `synonymSets`:

```json
{ "synonym_sets": ["workshop-synonyms"] }
```

3. Upsert a curation set named `workshop-curation`. Pin document id `pride` at position 1 when the query is exactly `classic`.

```json
{
  "items": [
    {
      "id": "pin-pride",
      "rule": { "query": "classic", "match": "exact" },
      "includes": [{ "id": "pride", "position": 1 }]
    }
  ]
}
```

4. The Java client can link synonym sets on update, but not curation sets. PATCH `/collections/{name}` with `{ "curation_sets": ["workshop-curation"] }`.

The client methods are `client.synonymSet("workshop-synonyms").upsert(...)` and `client.curationSet("workshop-curation").upsert(...)`. `PUT /synonym_sets/workshop-synonyms` and `PUT /curation_sets/workshop-curation` are the same calls over HTTP.

Run `./gradlew indexSessions` again.

- Search `scifi`. Dune, Neuromancer, and Foundation appear even though those records say "science fiction".
- Search `classic`. Pride and Prejudice is the first row.

## Exercise 6: a key that can only search

Fill in `exercise6SearchOnlyKey`.

1. Create an API key whose actions are only `documents:search` and whose collections list is only this collection.
2. Print `value` from the create response once.
3. Build a second client with that key and try `documents().create(...)`. The call must fail.

Copy the printed key into `typesense.searchApiKey` in `local.properties` and rebuild the app. Search still works. The admin key is still only used by the Gradle task.

That is the production rule. Ship a search-only key in the app. Do not ship the admin key.

## 45-minute cut

Stop after Exercise 3. You have typo tolerance, prefix search, filters, and facets. Synonyms, curation, and the search-only key are the second half.

## What we are not building in the room

These are real Typesense features. They need an embedding model, an LLM key, or a multi-node cluster, so they are a poor use of workshop time:

- Semantic and hybrid search, with built-in models such as E-5
- Natural language search, which turns a sentence into filters and sorts
- Conversational RAG
- Image search with CLIP, and voice search with Whisper
- Analytics and query suggestions
- Raft clustering for high availability

Geo search, grouping, federated multi-search, and JOINs are stretch demos. The book documents already have a `location` geopoint. See `INSTRUCTOR.md`.
