# Bookstore search with Typesense

Repo: https://github.com/djain2405/typesense-android-workshop

```bash
git clone https://github.com/djain2405/typesense-android-workshop.git
```

A native Android app: Kotlin, Jetpack Compose, and the Typesense Java client (`org.typesense:typesense-java:2.1.0`). It is not a Kotlin Multiplatform project.

You start with a working bookstore search, then turn on the Typesense features that change the screen: typo tolerance, prefix search, filters, facets, highlights, synonyms, curation, and a search-only API key.

Typesense is one C++ binary. It keeps the index in memory and answers typo-tolerant keyword search, plus vector search, in a single HTTP API. The phone app receives the host and a search key. The admin key stays in `local.properties` and is used by `./gradlew indexSessions`.

`main` is the starting project. `solution` is the same project with every step filled in: https://github.com/djain2405/typesense-android-workshop/tree/solution

Compare them with `git diff main..solution`.

## Setup

You need Android Studio, an emulator on API 26 or newer, and JDK 21. The app compiles for Java 17. The index task targets JVM 21, so `./gradlew indexSessions` needs JDK 21. Android Studio’s current JetBrains Runtime is 21.

1. Copy `local.properties.example` to `local.properties`.
2. Set `sdk.dir` to your Android SDK.
3. Pick a connection:

| Where Typesense runs | `typesense.host` | `typesense.port` | `typesense.protocol` |
| --- | --- | --- | --- |
| Docker, Android emulator | `10.0.2.2` | `8108` | `http` |
| Docker, physical phone | your laptop’s LAN IP | `8108` | `http` |
| Shared cluster | the cluster hostname, without `https://` | `443` | `https` |

On a shared cluster, set `typesense.collection` to `books-` plus your name. Put a search-only key in `typesense.searchApiKey`. Keep the admin key in `typesense.adminApiKey`.

4. Local server, when you are not using a shared cluster:

```bash
docker compose up -d
./gradlew indexSessions
```

Docker runs Typesense 30.1 on port 8108 with admin key `workshop-admin-key`. `curl http://localhost:8108/health` returns ok. The index task talks to `localhost` when the app host is `10.0.2.2`, because Gradle runs on the laptop and the emulator address is only for the phone.

5. Run the `app` configuration. The first search runs as soon as the screen opens. Debug builds allow cleartext HTTP. After any change to `local.properties`, rebuild so `BuildConfig` picks it up.

The screen is “Bookstore search.” The catalog is `data/books.json`: 10 books. That file is the source of truth. Typesense is the search index.

- The Hobbit and The Lord of the Rings, J.R.R. Tolkien, Fantasy, Central Books in San Francisco (`37.7749, -122.4194`). Ratings 4.8 and 4.9.
- Harry Potter and the Sorcerer's Stone, J.K. Rowling, Fantasy, Harbor Books in Oakland (`37.8044, -122.2712`). Rating 4.7.
- Dune, Neuromancer, and Foundation, Science Fiction. Dune’s description mentions Arrakis.
- Pride and Prejudice and Emma, Jane Austen, Classic.
- The Odyssey, Homer, Poetry. The description contains the word “classic.”
- The Murder of Roger Ackroyd, Agatha Christie, Mystery.

Genre counts: Fantasy 3, Science Fiction 3, Classic 2, Poetry 1, Mystery 1.

## Step 0. It already searches

No code changes. In `WorkshopSearch.kt`, `query_by` is `title,authors` and prefix is off.

- Type `tolkein`. The Hobbit and The Lord of the Rings appear. `tolkein` is one typo from `Tolkien`. Typesense allows one typo from 4 characters and two typos from 7.
- Type `Rowling`. Harry Potter and the Sorcerer's Stone appears, because authors are already in `query_by`.

If this search fails, fix the connection before changing any code. A blank host or a blank search key shows: “Copy local.properties.example to local.properties, then rebuild.”

## Step 1. Create the collection

Open `tools/src/main/kotlin/com/workshop/typesense/index/IndexWorkshop.kt` and read `exercise1CreateAndImport`. It creates the collection when it is missing and upserts `data/books.json`. `genre` and `store` are facetable. `rating` is sortable. `location` is a geopoint.

```bash
./gradlew indexSessions
```

You should see `Indexed 10 documents into 'books'.` Run it again. The count stays 10.

## Step 2. Choose the fields and turn on prefix search

Open `app/src/main/kotlin/com/workshop/typesense/WorkshopSearch.kt`.

`query_by` is the list of fields the query searches. Prefix is why search-as-you-type works. Typo tolerance is already on, which is why Step 0 worked. Authors are already in `query_by`. This step adds genre.

```kotlin
fun exercise2QueryBy(): String = "title,authors,genre"

fun exercise2Prefix(): String = "true"
```

Rebuild. Type `har`. Harry Potter appears before you finish the word. Type `fantasy`. The fantasy books appear, because genre is now a searched field.

## Step 3. Filter and facets

Still in `WorkshopSearch.kt`. `filter_by` is an exact match. `facet_by` supplies the counts on the chips. Synonyms apply to the query text. A filter matches the stored value.

```kotlin
fun exercise3Filter(genre: String?): String? {
    if (genre.isNullOrBlank() || genre == "All") return null
    return "genre:=$genre"
}

fun exercise3Facet(): String? = "genre"
```

Rebuild. The chips gain counts. Tap Fantasy and only the three fantasy books remain. Tap All and the filter goes away. The Fantasy filter value is `genre:=Fantasy`.

## Step 4. Highlights

Highlights are snippets with `<mark>` tags. The app turns those into bold text.

```kotlin
fun exercise4HighlightFields(): String? = "title,description"
```

Rebuild and search `arrakis`. The matched word is highlighted on Dune.

Relevance stays the default. Tap **By rating** to send `rating:desc`. That parameter is already wired in `exerciseSort`. The Lord of the Rings, rated 4.9, leads a rating sort of the full catalog.

## Step 5. Synonyms and curation

Back in `IndexWorkshop.kt`, fill in `exercise5SynonymsAndCuration`.

1. Upsert a synonym set named `workshop-synonyms` with one multi-way item, id `scifi-words`, synonyms `scifi` and `science fiction`.
2. Link that set to the collection:

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

4. Link the curation set with PATCH `/collections/{name}` and `{ "curation_sets": ["workshop-curation"] }`. The Java client links synonym sets on collection update. Linking a curation set is this PATCH.

```kotlin
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
patchCollection(collection, """{"curation_sets":["workshop-curation"]}""")
```

`client.synonymSet("workshop-synonyms").upsert(...)` and `client.curationSet("workshop-curation").upsert(...)` are `PUT /synonym_sets/workshop-synonyms` and `PUT /curation_sets/workshop-curation`.

Run `./gradlew indexSessions` again.

- Search `scifi`. Dune, Neuromancer, and Foundation appear. Those records say “science fiction.”
- Search `classic`. Pride and Prejudice is the first row.

A synonym set can be reused across collections. A curation set pins a hit for a query and leaves the documents unchanged. Pinning Pride and Prejudice for `classic` is merchandising. Emma is also a classic, and The Odyssey’s description contains the word “classic,” so the pin is visible at the top.

On a shared cluster, use your own collection name. The set names `workshop-synonyms` and `workshop-curation` are cluster-wide.

## Step 6. A key that can only search

Fill in `exercise6SearchOnlyKey`.

1. Create an API key whose actions are only `documents:search` and whose collections list is only this collection.
2. Print `value` from the create response once.
3. Build a second client with that key and try `documents().create(...)`. The call fails.

```kotlin
val created = client.keys().create(
    ApiKeySchema()
        .description("Workshop search only")
        .addActionsItem("documents:search")
        .addCollectionsItem(collection)
)
println(created.value)
```

Copy the printed key into `typesense.searchApiKey` in `local.properties` and rebuild the app. Search still works. The admin key is still only used by the Gradle task.

Ship a search-only key in the app. Keep the admin key on the machine that indexes.

## When the phone cannot reach the server

1. `docker compose up -d` on your laptop.
2. Emulator: `typesense.protocol=http`, `typesense.host=10.0.2.2`, `typesense.port=8108`.
3. Physical phone: `typesense.host` is the laptop’s LAN IP.
4. `./gradlew indexSessions`.
5. Rebuild the app.
6. `curl http://localhost:8108/health` returns ok.

## Where each step lives

- `WorkshopSearch.kt` — Steps 2, 3, and 4.
- `IndexWorkshop.kt` — Step 1 is already implemented. Steps 5 and 6.
- `WorkshopScreen.kt` — the screen.
- `TypesenseProvider.kt` — the client, built from `BuildConfig`.
- `data/books.json` — the catalog.
- `local.properties` — host, port, protocol, collection, search key, admin key. Gitignored.

## Where to go next

These are real Typesense features. Each one needs an embedding model, an LLM key, or a multi-node cluster, so they sit outside these steps: semantic and hybrid search (built-in models such as E5), natural language search, conversational RAG, image search with CLIP, voice search with Whisper, analytics and query suggestions, and Raft clustering. The feature list is at https://typesense.org/docs/overview/features.html

The book documents already have a `location` geopoint. From a temporary change in `exercise3Filter`:

```text
location:(37.7749, -122.4194, 5 km)
```

That keeps Central Books and drops Harbor Books in Oakland (`37.8044, -122.2712`). Widen the radius and Harbor Books comes back. Revert the temporary filter when you are done.

`group_by=genre&group_limit=1` returns a single book per genre.

`POST /multi_search` can query `books` and a second collection in one HTTP call. An `author_id` reference field can pull an author document at query time with a JOIN. This dataset keeps the author name on the book, so these steps stand on one collection.
