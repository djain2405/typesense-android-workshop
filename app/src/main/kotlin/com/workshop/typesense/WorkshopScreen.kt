package com.workshop.typesense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val Ink = Color(0xFF1F6B4A)
private val Highlight = Color(0xFFFFE08A)

class WorkshopViewModel : ViewModel() {
    var query by mutableStateOf("")
    var genre by mutableStateOf("All")
    var sortByRating by mutableStateOf(false)
    var result by mutableStateOf(WorkshopResult(emptyList(), emptyList(), 0))
    var error by mutableStateOf<String?>(null)

    private val searcher = workshopClient()?.let { WorkshopSearch(it, workshopCollection()) }
    private var searchJob: Job? = null

    init {
        onQueryChange("")
    }

    fun onQueryChange(value: String) {
        query = value
        scheduleSearch()
    }

    fun onGenre(value: String) {
        genre = value
        scheduleSearch()
    }

    fun onSortByRating(value: Boolean) {
        sortByRating = value
        scheduleSearch()
    }

    private fun scheduleSearch() {
        val current = searcher
        if (current == null) {
            error = "Copy local.properties.example to local.properties, then rebuild."
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(200)
            error = null
            try {
                result = withContext(Dispatchers.IO) {
                    current.search(query, genre, sortByRating)
                }
            } catch (exception: Exception) {
                error = exception.message ?: "Search failed"
            }
        }
    }
}

@Composable
fun WorkshopScreen(viewModel: WorkshopViewModel = viewModel()) {
    MaterialTheme(colorScheme = lightColorScheme(primary = Ink)) {
        Scaffold { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    "Bookstore search",
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Search powered by Typesense",
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                OutlinedTextField(
                    value = viewModel.query,
                    onValueChange = viewModel::onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    singleLine = true,
                    placeholder = { Text("Search books") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.onQueryChange(viewModel.query) })
                )
                GenreRow(
                    facets = viewModel.result.facets,
                    selected = viewModel.genre,
                    onSelect = viewModel::onGenre
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !viewModel.sortByRating,
                        onClick = { viewModel.onSortByRating(false) },
                        label = { Text("Relevance") }
                    )
                    FilterChip(
                        selected = viewModel.sortByRating,
                        onClick = { viewModel.onSortByRating(true) },
                        label = { Text("By rating") }
                    )
                    Text(
                        "${viewModel.result.found} results",
                        modifier = Modifier.padding(top = 12.dp),
                        color = Color.DarkGray
                    )
                }
                viewModel.error?.let { message ->
                    Text(
                        message,
                        modifier = Modifier.padding(top = 8.dp),
                        color = Color(0xFFB00020)
                    )
                }
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.result.hits, key = { it.id }) { hit ->
                        BookCard(hit)
                    }
                }
            }
        }
    }
}

@Composable
private fun GenreRow(
    facets: List<GenreFacet>,
    selected: String,
    onSelect: (String) -> Unit
) {
    val genres = listOf("All") + if (facets.isEmpty()) {
        listOf("Fantasy", "Science Fiction", "Classic", "Mystery", "Poetry")
    } else {
        facets.map { it.name }
    }
    LazyRow(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(genres) { name ->
            val count = facets.firstOrNull { it.name == name }?.count
            val label = if (count == null || name == "All") name else "$name $count"
            Button(
                onClick = { onSelect(name) },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected == name) Ink else Color.White,
                    contentColor = if (selected == name) Color.White else Ink
                )
            ) {
                Text(label)
            }
        }
    }
}

@Composable
private fun BookCard(hit: BookHit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F7F4))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(hit.genre, color = Ink, fontWeight = FontWeight.Bold)
            Text(hit.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(hit.authors)
            if (hit.rating.isNotBlank()) {
                Text("Rating ${hit.rating}")
            }
            if (hit.snippet.isNotBlank()) {
                Text(highlightSnippet(hit.snippet), modifier = Modifier.padding(top = 4.dp))
            }
            Text(hit.store, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

fun highlightSnippet(snippet: String): AnnotatedString = buildAnnotatedString {
    var remaining = snippet
    while (remaining.isNotEmpty()) {
        val start = remaining.indexOf("<mark>")
        if (start < 0) {
            append(remaining)
            break
        }
        append(remaining.substring(0, start))
        val after = remaining.substring(start + "<mark>".length)
        val end = after.indexOf("</mark>")
        if (end < 0) {
            append(after)
            break
        }
        pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, background = Highlight))
        append(after.substring(0, end))
        pop()
        remaining = after.substring(end + "</mark>".length)
    }
}
