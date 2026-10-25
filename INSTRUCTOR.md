# Instructor guide

Two hours, or a 45-minute cut that ends after track facets. Attendees are Android developers. Stay on the phone screen. Do not tour the Typesense dashboard until Exercise 6.

## Before the room

1. Start a Typesense 30.1 cluster, or run `docker compose up -d` on the projector laptop.
2. Run `./gradlew indexSessions` once against the collection name you will put in the starter `local.properties`. Exercise 0 has to search before anyone edits code.
3. If you use a shared cluster, create a search-only key for the starter and a separate admin key for yourself. Do not put the admin key in the app's `typesense.searchApiKey`.
4. Ask people to use `sessions-<their-name>` on a shared cluster. The index task upserts, but two people on the same collection will fight over synonyms.
5. Open the solution branch only if someone is blocked. `main` is the starter. `solution` has Exercises 2 through 6 filled in.

## Clock

- 10 min. What Typesense is. One binary, in memory, typo-tolerant keyword search and vector search behind one HTTP API. The alternative is a hosted keyword product plus a separate vector database. Mention the features you will not build: semantic search, RAG, image search, voice search, analytics, Raft. Point at https://typesense.org/docs/overview/features.html
- 15 min. Exercise 0. Everyone types `compse` and sees the Compose talk. This is the keynote demo. If it fails, fix the network now.
- 70 min. Exercises 1 through 6. Each one is a function with the expected query written above it.
- 15 min. Synonyms, curation, and the admin key. Exercise 5 and 6 are this block if the room is running long. The point of Exercise 6 is the store lesson: the admin key indexed the conference app during development and is absent from the release binary.
- 10 min. Stretch map and questions.

45-minute cut: the 10 minute intro, Exercise 0, Exercise 1 as a live run, then Exercises 2 and 3. Skip 4 through 6 and name them on the last slide.

## What to say at each exercise

Exercise 1. The JSON file is the source of truth. Typesense is an index you can drop and rebuild. Show the document count staying at 10 on a second run.

Exercise 2. `query_by` is the list of fields. Prefix is why search-as-you-type works. Typo tolerance was already on, which is why Exercise 0 worked with no code.

Exercise 3. `filter_by` is exact. `facet_by` is the counts. Synonyms do not apply inside filters.

Exercise 4. Highlights are snippets with `<mark>` tags. The app turns those into bold text. This is what the conference app left out.

Exercise 5. A synonym set is reusable across collections. A curation set pins, hides, or replaces hits for a query without editing the documents. Pinning `welcome` is merchandising, not relevance.

Exercise 6. `documents:search` cannot import. After they copy the new key into the app and rebuild, search still works. Writes do not.

## When the phone cannot reach the server

Do this before any exercise code, or Exercise 0 never lands.

1. `docker compose up -d` on the attendee's laptop.
2. Emulator: `typesense.protocol=http`, `typesense.host=10.0.2.2`, `typesense.port=8108`. Debug builds allow cleartext. Release builds do not.
3. Physical phone: `typesense.host` is the laptop's LAN IP, not `localhost` and not `10.0.2.2`.
4. `./gradlew indexSessions`. Gradle rewrites `10.0.2.2` to `localhost` because the task runs on the laptop.
5. Rebuild the app so BuildConfig picks up `local.properties`.

`curl http://localhost:8108/health` should return ok before you look at the app.

## Stretch demo, after the exercises

Do these from a temporary change in `exercise3Filter`, then revert. Do not make the room type them.

Geo, around the National ICT Innovation Hub. Documents use `[latitude, longitude]`.

```text
location:(0.3328, 32.6128, 2 km)
```

The lobby meetup at Royal Suites (`0.3155, 32.6205`) drops out. Widen the radius and it comes back.

Grouping. One search parameter, `group_by=track&group_limit=1`, shows a single talk per track.

Federated search. `POST /multi_search` can query `sessions` and a second `speakers` collection in one HTTP call. Say it. Do not build the second collection unless you have time.

JOIN. A `speaker_id` reference field on sessions can pull the speaker document at query time. The dataset keeps the speaker name on the session so the required exercises do not need a second collection.

## Solution branch

```bash
git diff main..solution
```

Exercises 2, 3, and 4 change `WorkshopSearch.kt`. Exercises 5 and 6 change `IndexWorkshop.kt`.
